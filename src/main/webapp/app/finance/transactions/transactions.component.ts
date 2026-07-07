import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, NgZone, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import {
  CellClickedEvent,
  ColDef,
  ColGroupDef,
  GridOptions,
  GridReadyEvent,
  IDatasource,
  IGetRowsParams,
  ModelUpdatedEvent,
  RowModelType,
  type Theme,
} from 'ag-grid-community';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { Transactions } from './transactions.service';
import { FinancialAccount, FinancialTransaction } from '../finance.model';
import { formatCurrency } from '@angular/common';
import { AccountList } from '../account-list/account-list.service';
import { CookieService } from 'ngx-cookie';
import { AgGridThemeName, AgGridThemeService } from 'app/shared/ag-grid/ag-grid-theme.service';
import SharedModule from 'app/shared/shared.module';
import { NgApexchartsModule } from 'ng-apexcharts';
import { OverlayOnShowEvent } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { AutoCompleteCompleteEvent, AutoCompleteModule } from 'primeng/autocomplete';
import { TreeSelectModule } from 'primeng/treeselect';
import { TransactionEditorComponent } from './transaction-editor.component';
import { TransactionImportService } from '../account-list/transaction-import.service';
import {
  TransactionEditorDraftRequest,
  TransactionEditorMode,
  TransactionEditorSelectableType,
  TransactionGridQuery,
  TransactionOption,
  TransactionTreeOption,
  TransactionUpdate,
} from './transactions.types';
import { TransactionImportDraft, TransactionImportHistoryItem, TransactionImportRow } from '../account-list/transaction-import.types';

interface TransactionColumnOption {
  id: string;
  label: string;
  defaultVisible: boolean;
}

interface AccountSelectGroup {
  label: string;
  items: FinancialAccount[];
}

interface ImportGridFocusTarget {
  transactionId: string;
  rowIndex: number;
}

@Component({
  selector: 'jhi-transactions',
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.scss'],
  imports: [
    SharedModule,
    NgApexchartsModule,
    AgGridAngular,
    TransactionEditorComponent,
    DialogModule,
    AutoCompleteModule,
    TreeSelectModule,
  ],
})
export class TransactionsComponent implements OnInit {
  private static readonly IMPORT_LOOKUP_PAGE_SIZE = 50;
  static PREV_TXN_ACCOUNT_ID = 'previous-txn-account-id';
  static SHOW_FILTER_ROW = 'transactions-show-filter-row';
  static COLUMN_VISIBILITY = 'transactions-column-visibility';
  static SHOW_CLOSED_ACCOUNTS = 'transactions-show-closed-accounts';
  static readonly DEFAULT_SORT = ['date,desc', 'number,desc'];
  title = '';
  theme = 'light';
  agGridTheme: AgGridThemeName = 'alpine';
  agGridThemeDefinition: Theme;
  isLoading = false;
  accountId: string | null = null;
  account: FinancialAccount | null = null;

  totalItems = 0;
  itemsPerPage = 100;
  page!: number;
  predicate!: string;
  ascending!: boolean;

  balance = -1;
  isEditorOpen = false;
  isSavingTransaction = false;
  saveTransactionError: string | null = null;
  showFilterRow = false;
  editorMode: TransactionEditorMode = 'view';
  editorInitialTransactionType: TransactionEditorSelectableType | null = null;
  selectedTransactionId: string | null = null;
  selectedTransaction: FinancialTransaction | null = null;
  readonly columnToggleOptions: TransactionColumnOption[] = [
    { id: 'date', label: 'Date', defaultVisible: true },
    { id: 'payee', label: 'Payee', defaultVisible: true },
    { id: 'security', label: 'Security', defaultVisible: true },
    { id: 'memo', label: 'Memo', defaultVisible: false },
    { id: 'who', label: 'Who', defaultVisible: false },
    { id: 'amount', label: 'Amount', defaultVisible: false },
    { id: 'category', label: 'Category', defaultVisible: false },
    { id: 'tags', label: 'Tags', defaultVisible: true },
    { id: 'payment', label: 'Payment', defaultVisible: true },
    { id: 'deposit', label: 'Deposit', defaultVisible: true },
    { id: 'runningBalance', label: 'Balance', defaultVisible: true },
  ];
  private visibleColumnIds = new Set(this.columnToggleOptions.filter(option => option.defaultVisible).map(option => option.id));
  public columnDefs: (ColDef | ColGroupDef)[] = this.buildColumnDefs();

  public defaultColDef: ColDef = {
    sortable: true,
    filter: true,
    floatingFilter: false,
    flex: 1,
    resizable: true,
    wrapText: false,
    filterParams: {
      buttons: ['reset'],
      closeOnApply: true,
      debounceMs: 300,
    },
  };

  public transactions!: FinancialTransaction[] | null;

  public accounts!: FinancialAccount[] | null;
  accountSelectOptions: AccountSelectGroup[] = [];
  transferAccountOptions: TransactionOption[] = [];
  showClosedAccounts = false;
  isUpdatingAccountFavourite = false;
  importDialogVisible = false;
  importDialogTab: 'new' | 'history' = 'new';
  importRawContent = '';
  importRawHtml: string | null = null;
  expectedEndingBalance: string | number | null = '';
  importBusy = false;
  importError: string | null = null;
  importDraft: TransactionImportDraft | null = null;
  importHistory: TransactionImportHistoryItem[] = [];
  importHistoryLoading = false;
  payeeSuggestions: Record<string, TransactionOption[]> = {};
  importTransferAccountSuggestions: Record<string, TransactionOption[]> = {};
  importCategoryOptions: TransactionTreeOption[] = [];
  importCategoryLoading = false;
  duplicatePreviewVisible = false;
  duplicatePreviewLoading = false;
  duplicatePreviewTransaction: FinancialTransaction | null = null;
  duplicatePreviewRow: TransactionImportRow | null = null;
  showDuplicateRows = false;
  showConfirmedDuplicateRows = false;
  showAcceptedRows = false;
  showIgnoredRows = false;
  importProgressMessages = [
    'Reading the pasted layout',
    'Extracting transaction rows',
    'Matching payees and categories',
    'Checking balances and duplicates',
  ];
  importProgressMessageIndex = 0;
  private importProgressTimer: ReturnType<typeof setInterval> | null = null;
  private pendingImportGridFocus: ImportGridFocusTarget | null = null;
  private pendingSelectedTransactionFocusId: string | null = null;
  private importStateAccountId: string | null = null;

  @ViewChild(AgGridAngular) agGrid?: AgGridAngular;

  rowModelType: RowModelType = 'infinite';

  gridOptions: GridOptions = {
    pagination: false,
    rowSelection: 'single',
    rowModelType: this.rowModelType,
    cacheBlockSize: this.itemsPerPage,
    paginationPageSize: this.itemsPerPage,
  };

  dataSource: IDatasource = {
    getRows: (params: IGetRowsParams) => {
      if (this.accountId) {
        this.isLoading = true;
        const req = this.buildGridQuery(params);
        this.transactionService.get(this.accountId, req).subscribe({
          next: (res: HttpResponse<FinancialTransaction[]>) => {
            this.isLoading = false;
            this.onSuccess(res.body, res.headers);
            const data = res.body!;
            params.successCallback(data, this.totalItems);
          },
          error: () => {
            this.isLoading = false;
            params.failCallback();
          },
        });
      }
    },
    rowCount: undefined,
  };

  private readonly transactionService = inject(Transactions);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly themeService = inject(ThemeService);
  private readonly accountService = inject(AccountList);
  private readonly cookieService = inject(CookieService);
  private readonly agGridThemeService = inject(AgGridThemeService);
  private readonly transactionImportService = inject(TransactionImportService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly ngZone = inject(NgZone);

  constructor() {
    this.agGridThemeDefinition = this.agGridThemeService.getThemeDefinition(this.agGridTheme, this.theme);
  }

  ngOnInit(): void {
    this.theme = this.themeService.theme();
    this.agGridTheme = this.agGridThemeService.theme();
    this.agGridThemeDefinition = this.agGridThemeService.getThemeDefinition(this.agGridTheme, this.theme);
    this.showFilterRow = this.getCookie(TransactionsComponent.SHOW_FILTER_ROW) === 'true';
    this.showClosedAccounts = this.getCookie(TransactionsComponent.SHOW_CLOSED_ACCOUNTS) === 'true';
    this.loadColumnVisibility();
    this.defaultColDef = { ...this.defaultColDef, floatingFilter: this.showFilterRow };
    this.columnDefs = this.buildColumnDefs();
    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
      this.agGridThemeDefinition = this.agGridThemeService.getThemeDefinition(this.agGridTheme, this.theme);
    });
    this.agGridThemeService.onChange.subscribe(theme => {
      this.agGridTheme = theme;
      this.agGridThemeDefinition = this.agGridThemeService.getThemeDefinition(theme, this.theme);
    });

    this.title = this.getPageTitle(this.router.routerState.snapshot.root);

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.accountId = params['id'];
        if (this.accountId) {
          this.account = this.getAccount(this.accountId);
        }
      }
    });

    this.accountService.getSimple().subscribe({
      next: (res: FinancialAccount[]) => {
        this.accounts = this.sortAccounts(res);
        this.accountSelectOptions = this.buildAccountSelectOptions();
        this.transferAccountOptions = this.buildTransferAccountOptions();
        if (this.accountId != null) {
          this.account = this.getAccount(this.accountId);
        }
      },
    });

    if (this.accountId == null) {
      const prevTxnAccountId = this.getCookie(TransactionsComponent.PREV_TXN_ACCOUNT_ID);
      if (prevTxnAccountId) {
        this.accountId = prevTxnAccountId;
      }
    }
  }

  onChangeAccount(_event: any): void {
    this.balance = -1;
    this.closeEditor();
    if (this.importStateAccountId && this.importStateAccountId !== this.accountId) {
      this.resetImportDialogState({ discardDraft: true, closeDialog: true });
    }
    this.accountSelectOptions = this.buildAccountSelectOptions();
    if (this.accountId) {
      this.account = this.getAccount(this.accountId);
      this.transferAccountOptions = this.buildTransferAccountOptions();
      this.agGrid?.api.setGridOption('datasource', this.dataSource);
      this.saveCookie(TransactionsComponent.PREV_TXN_ACCOUNT_ID, this.accountId);
    }
  }

  toggleFilterRow(): void {
    this.showFilterRow = !this.showFilterRow;
    this.defaultColDef = { ...this.defaultColDef, floatingFilter: this.showFilterRow };
    this.agGrid?.api.setGridOption('defaultColDef', this.defaultColDef);
    this.refreshColumnDefs();
    this.saveCookie(TransactionsComponent.SHOW_FILTER_ROW, String(this.showFilterRow));
  }

  toggleShowClosedAccounts(): void {
    this.showClosedAccounts = !this.showClosedAccounts;
    this.accountSelectOptions = this.buildAccountSelectOptions();
    this.saveCookie(TransactionsComponent.SHOW_CLOSED_ACCOUNTS, String(this.showClosedAccounts));
  }

  toggleSelectedAccountFavourite(): void {
    if (!this.account) {
      return;
    }

    const accountId = this.account.id;
    const previousFavourite = this.account.favourite;
    const nextFavourite = !this.account.favourite;
    this.accounts =
      this.accounts?.map(account => (account.id === accountId ? { ...account, favourite: nextFavourite } : account)) ?? this.accounts;
    this.accounts = this.accounts ? this.sortAccounts(this.accounts) : this.accounts;
    this.account = this.getAccount(accountId);
    this.accountSelectOptions = this.buildAccountSelectOptions();
    this.transferAccountOptions = this.buildTransferAccountOptions();
    this.isUpdatingAccountFavourite = true;
    this.accountService.updateFavourite(accountId, nextFavourite).subscribe({
      next: updated => {
        this.isUpdatingAccountFavourite = false;
        this.accounts =
          this.accounts?.map(account => (account.id === updated.id ? { ...account, favourite: updated.favourite } : account)) ??
          this.accounts;
        this.accounts = this.accounts ? this.sortAccounts(this.accounts) : this.accounts;
        this.account = this.getAccount(updated.id);
        this.accountSelectOptions = this.buildAccountSelectOptions();
        this.transferAccountOptions = this.buildTransferAccountOptions();
      },
      error: () => {
        this.isUpdatingAccountFavourite = false;
        this.accounts =
          this.accounts?.map(account => (account.id === accountId ? { ...account, favourite: previousFavourite } : account)) ??
          this.accounts;
        this.accounts = this.accounts ? this.sortAccounts(this.accounts) : this.accounts;
        this.account = this.getAccount(accountId);
        this.accountSelectOptions = this.buildAccountSelectOptions();
        this.transferAccountOptions = this.buildTransferAccountOptions();
      },
    });
  }

  isColumnVisible(columnId: string): boolean {
    return this.visibleColumnIds.has(columnId);
  }

  toggleColumnVisibility(columnId: string, visible: boolean): void {
    if (visible) {
      this.visibleColumnIds.add(columnId);
    } else {
      this.visibleColumnIds.delete(columnId);
    }

    this.persistColumnVisibility();
    this.refreshColumnDefs();
  }

  getAccountName(id: string): string {
    let val = '[UNKNOWN]';
    this.accounts?.every(element => {
      if (element.id === id) {
        val = element.name;
        return false;
      }
      return true;
    });
    return val;
  }

  getAccount(id: string): FinancialAccount | null {
    let val = null;
    this.accounts?.every(element => {
      if (element.id === id) {
        val = element;
        return false;
      }
      return true;
    });
    return val;
  }

  accountGroupValue(group: AccountSelectGroup): string {
    return `${group.label} (${group.items.length})`;
  }

  onGridReady(params: GridReadyEvent): void {
    params.api.setGridOption('datasource', this.dataSource);
  }

  onModelUpdated(_event: ModelUpdatedEvent<any>): void {
    this.autoSize();
    this.applyPendingSelectedTransactionFocus();
    this.applyPendingImportGridFocus();
  }

  onCellClicked(e: CellClickedEvent): void {
    if (!e.data) {
      return;
    }

    if (e.colDef.colId === 'security' && e.data.securityId) {
      void this.router.navigate(['/investment', e.data.securityId]);
      return;
    }

    this.saveTransactionError = null;
    this.selectedTransactionId = e.data.id;
    this.selectedTransaction = this.processTransaction({ ...e.data });
    this.editorMode = 'view';
    this.isEditorOpen = true;
    e.node.setSelected(true, true);
    this.keepSelectedRowVisible(e.node.rowIndex ?? null);
  }

  openAddTransaction(request?: TransactionEditorDraftRequest | void): void {
    if (!this.accountId || !this.account) {
      return;
    }

    this.saveTransactionError = null;
    this.isEditorOpen = true;
    this.editorMode = 'add';
    this.editorInitialTransactionType = request?.initialTransactionType ?? null;
    this.selectedTransactionId = null;
    this.selectedTransaction = this.createDraftTransaction(request);
    this.agGrid?.api.deselectAll();
  }

  openEditMode(): void {
    if (!this.selectedTransaction || this.getSelectedTransactionReadonlyReason()) {
      return;
    }

    this.editorMode = 'edit';
    this.editorInitialTransactionType = null;
  }

  closeEditor(): void {
    this.isEditorOpen = false;
    this.editorMode = 'view';
    this.editorInitialTransactionType = null;
    this.saveTransactionError = null;
    this.selectedTransactionId = null;
    this.selectedTransaction = null;
    this.agGrid?.api.deselectAll();
  }

  saveTransaction(update: TransactionUpdate): void {
    if (!this.accountId) {
      return;
    }

    const previousAmount = this.editorMode === 'add' ? 0 : (this.selectedTransaction?.amount ?? 0);

    this.saveTransactionError = null;
    this.isSavingTransaction = true;
    const request =
      this.editorMode === 'add' || !this.selectedTransactionId
        ? this.transactionService.create(this.accountId, update)
        : this.transactionService.update(this.accountId, this.selectedTransactionId, update);

    request.subscribe({
      next: transaction => {
        this.isSavingTransaction = false;
        this.editorMode = 'view';
        this.editorInitialTransactionType = null;
        this.selectedTransactionId = transaction.id;
        this.selectedTransaction = this.processTransaction({ ...transaction });
        this.refreshSelectedRow(this.selectedTransaction);
        if (this.balance !== -1) {
          this.balance += transaction.amount - previousAmount;
        }
        this.agGrid?.api.refreshInfiniteCache();
      },
      error: (error: HttpErrorResponse) => {
        this.isSavingTransaction = false;
        this.saveTransactionError = this.getSaveTransactionErrorMessage(error);
        console.error('Transaction save failed', {
          accountId: this.accountId,
          selectedTransactionId: this.selectedTransactionId,
          update,
          error,
        });
      },
    });
  }

  deleteSelectedTransaction(): void {
    if (!this.accountId || !this.selectedTransactionId || this.editorMode === 'add') {
      return;
    }

    const deletedAmount = this.selectedTransaction?.amount ?? 0;
    this.saveTransactionError = null;
    this.isSavingTransaction = true;
    this.transactionService.delete(this.accountId, this.selectedTransactionId).subscribe({
      next: () => {
        this.isSavingTransaction = false;
        if (this.balance !== -1) {
          this.balance -= deletedAmount;
        }
        this.closeEditor();
        this.agGrid?.api.refreshInfiniteCache();
      },
      error: (error: HttpErrorResponse) => {
        this.isSavingTransaction = false;
        this.saveTransactionError = this.getSaveTransactionErrorMessage(error);
        console.error('Transaction delete failed', {
          accountId: this.accountId,
          selectedTransactionId: this.selectedTransactionId,
          error,
        });
      },
    });
  }

  getSelectedTransactionReadonlyReason(): string | null {
    if (!this.selectedTransaction || this.editorMode === 'add') {
      return null;
    }

    if (this.selectedTransaction.voided) {
      return 'Voided transactions are read-only for now.';
    }
    if (this.selectedTransaction.splitChild) {
      return 'Split child transactions are edited from their parent transaction.';
    }

    return null;
  }

  autoSize(): void {
    // Intentionally left for future column autosize work.
  }

  adjustedBalanceForCurrency(balance: number): number {
    if (this.account?.currencyCode !== 'AUD' && this.account?.fxRateToLocal != null) {
      return balance * this.account.fxRateToLocal;
    }

    return balance;
  }

  openLinkedTransfer(): void {
    if (!this.accountId || !this.selectedTransactionId) {
      return;
    }

    this.saveTransactionError = null;
    this.isSavingTransaction = true;
    this.transactionService.getLinkedTransfer(this.accountId, this.selectedTransactionId).subscribe({
      next: transaction => {
        this.isSavingTransaction = false;
        this.openLinkedTransferTransaction(transaction);
      },
      error: (error: HttpErrorResponse) => {
        this.isSavingTransaction = false;
        this.saveTransactionError = this.getSaveTransactionErrorMessage(error);
      },
    });
  }

  openImportDialog(tab: 'new' | 'history' = 'new'): void {
    if (!this.accountId || !this.account) {
      return;
    }

    if (this.importStateAccountId && this.importStateAccountId !== this.accountId) {
      this.resetImportDialogState({ discardDraft: true, closeDialog: true });
    }

    this.importStateAccountId = this.accountId;
    this.importDialogVisible = true;
    if (
      !this.importDraft &&
      !this.importRawContent &&
      !this.importRawHtml &&
      (this.expectedEndingBalance == null || this.expectedEndingBalance === '')
    ) {
      this.importDialogTab = tab;
    }
    this.duplicatePreviewVisible = false;
    this.duplicatePreviewLoading = false;
    this.duplicatePreviewTransaction = null;
    this.duplicatePreviewRow = null;
    console.log('[txn-import-ui] dialog:open', {
      accountId: this.accountId,
      tab: this.importDialogTab,
      hasDraft: Boolean(this.importDraft),
      importBusy: this.importBusy,
    });
    if (this.importBusy) {
      this.startImportProgress();
    }
    this.loadImportCategoryTreeOptions();
    this.loadImportHistory();
  }

  closeImportDialog(): void {
    this.importDialogVisible = false;
    this.stopImportProgress();
    this.duplicatePreviewVisible = false;
    this.duplicatePreviewLoading = false;
    this.duplicatePreviewTransaction = null;
    this.duplicatePreviewRow = null;
    console.log('[txn-import-ui] dialog:close', {
      accountId: this.accountId,
      hasDraft: Boolean(this.importDraft),
      importBusy: this.importBusy,
    });
  }

  createImportDraft(): void {
    if (!this.accountId || !this.account || !this.importRawContent.trim()) {
      this.importError = 'Paste transaction content before starting the import.';
      return;
    }

    const expectedEndingBalance = this.normalizeExpectedEndingBalance(this.expectedEndingBalance);
    this.importBusy = true;
    this.importError = null;
    console.log('[txn-import-ui] createDraft:start', {
      accountId: this.accountId,
      rawLength: this.importRawContent.length,
    });
    this.startImportProgress();
    this.transactionImportService
      .createDraft(this.accountId, {
        rawContent: this.importRawContent,
        rawHtml: this.importRawHtml,
        expectedEndingBalance: expectedEndingBalance != null && !Number.isNaN(expectedEndingBalance) ? expectedEndingBalance : null,
      })
      .subscribe({
        next: draft => {
          console.log('[txn-import-ui] createDraft:subscribe-next', { importId: draft.importId, rows: draft.rows?.length ?? 0 });
          this.applyImportDraft('post-response', draft);
        },
        error: (error: HttpErrorResponse) => {
          if (!this.importBusy) {
            return;
          }
          console.error('[txn-import-ui] createDraft:subscribe-error', error);
          this.importBusy = false;
          this.stopImportProgress();
          this.importError = this.getImportErrorMessage(error);
          this.changeDetectorRef.detectChanges();
        },
      });
  }

  onImportPaste(event: ClipboardEvent): void {
    const clipboardData = event.clipboardData;
    if (!clipboardData) {
      return;
    }

    const plainText = clipboardData.getData('text/plain');
    const html = clipboardData.getData('text/html');
    this.importRawHtml = this.trimToNull(html);

    if (plainText) {
      event.preventDefault();
      const target = event.target as HTMLTextAreaElement | null;
      const selectionStart = target?.selectionStart ?? this.importRawContent.length;
      const selectionEnd = target?.selectionEnd ?? this.importRawContent.length;
      this.importRawContent = this.importRawContent.slice(0, selectionStart) + plainText + this.importRawContent.slice(selectionEnd);

      requestAnimationFrame(() => {
        if (target) {
          const nextCursor = selectionStart + plainText.length;
          target.selectionStart = nextCursor;
          target.selectionEnd = nextCursor;
        }
      });
    }

    console.log('[txn-import-ui] paste:capture', {
      plainLength: plainText?.length ?? 0,
      htmlLength: html?.length ?? 0,
      hasHtml: Boolean(this.importRawHtml),
    });
  }

  reEvaluateDuplicate(row: TransactionImportRow): void {
    row.duplicateConfirmed = false;
    row.duplicateRejected = false;
    row.accepted = false;
    row.ignored = false;
    row.reviewMessage = null;
    console.log('[txn-import-ui] duplicate:reevaluate', { rowId: row.id, duplicateTransactionId: row.duplicateTransactionId });
    this.changeDetectorRef.detectChanges();
    this.saveImportRowWithOptions(row, { duplicateConfirmed: false, duplicateRejected: false });
  }

  agreeDuplicate(row: TransactionImportRow): void {
    row.reviewMessage = null;
    row.duplicateConfirmed = true;
    row.duplicateRejected = false;
    row.accepted = true;
    row.ignored = false;
    console.log('[txn-import-ui] duplicate:agree', { rowId: row.id, duplicateTransactionId: row.duplicateTransactionId });
    this.changeDetectorRef.detectChanges();
    this.saveImportRowWithOptions(row, {
      accepted: true,
      ignored: false,
      applyDuplicateResolution: true,
      duplicateConfirmed: true,
      duplicateRejected: false,
    });
  }

  markNotDuplicate(row: TransactionImportRow): void {
    row.reviewMessage = null;
    row.duplicateConfirmed = false;
    row.duplicateRejected = true;
    row.accepted = false;
    row.ignored = false;
    console.log('[txn-import-ui] duplicate:not-duplicate', { rowId: row.id, duplicateTransactionId: row.duplicateTransactionId });
    this.changeDetectorRef.detectChanges();
    this.saveImportRowWithOptions(row, {
      accepted: false,
      ignored: false,
      duplicateConfirmed: false,
      duplicateRejected: true,
    });
  }

  acceptImportRow(row: TransactionImportRow): void {
    const missingFields = this.getMissingRequiredReviewFields(row);
    if (missingFields.length > 0) {
      row.reviewMessage = `Please add ${missingFields.join(' and ')}`;
      this.changeDetectorRef.detectChanges();
      return;
    }

    row.reviewMessage = null;
    row.accepted = true;
    row.ignored = false;
    row.duplicateRejected = false;
    console.log('[txn-import-ui] review:accept', { rowId: row.id });
    this.changeDetectorRef.detectChanges();
    this.saveImportRowWithOptions(row, {
      accepted: true,
      ignored: false,
      duplicateRejected: false,
    });
  }

  ignoreImportRow(row: TransactionImportRow): void {
    row.accepted = false;
    row.ignored = true;
    row.duplicateConfirmed = false;
    row.reviewMessage = null;
    console.log('[txn-import-ui] review:ignore', { rowId: row.id });
    this.changeDetectorRef.detectChanges();
    this.saveImportRowWithOptions(row, { accepted: false, ignored: true, duplicateConfirmed: false });
  }

  reassessImportRow(row: TransactionImportRow): void {
    row.accepted = false;
    row.ignored = false;
    row.reviewMessage = null;
    console.log('[txn-import-ui] review:reassess', { rowId: row.id });
    this.changeDetectorRef.detectChanges();
    this.saveImportRowWithOptions(row, { accepted: false, ignored: false });
  }

  clearAllImportMemos(): void {
    if (!this.importDraft) {
      return;
    }

    this.importDraft = {
      ...this.importDraft,
      rows: this.importDraft.rows.map(row => ({
        ...row,
        memo: '',
      })),
    };
    this.changeDetectorRef.detectChanges();
  }

  startImportOver(): void {
    this.resetImportDialogState({ discardDraft: true, closeDialog: false });
  }

  saveImportRowWithOptions(
    row: TransactionImportRow,
    options?: {
      applyDuplicateResolution?: boolean;
      duplicateConfirmed?: boolean;
      duplicateRejected?: boolean;
      accepted?: boolean;
      ignored?: boolean;
    },
  ): void {
    if (!this.importDraft) {
      return;
    }

    const normalizedPayee = this.normalizeImportPayeeValue(row.payeeControl ?? row.payeeText ?? null);
    const normalizedTransferAccount = this.normalizeTransferAccountValue(row.transferAccountControl ?? null);
    row.resolvedPayeeId = normalizedPayee.id;
    row.resolvedPayeeName = normalizedPayee.name;
    row.payeeText = normalizedPayee.name;
    row.resolvedTransferAccountId = normalizedTransferAccount.id;
    row.resolvedTransferAccountName = normalizedTransferAccount.name;
    row.externalTransferLike = normalizedTransferAccount.id ? false : row.externalTransferLike;
    row.resolvedCategoryId = this.normalizeImportCategoryValue(row.categoryControl ?? null);
    row.resolvedCategoryName = row.categoryControl
      ? this.getCategoryTreeSelectLabel(row.categoryControl, row.aiCategoryGuess || 'Category')
      : null;

    this.importBusy = true;
    this.importError = null;
    this.transactionImportService
      .updateRow(this.importDraft.importId, row.id, {
        date: row.date,
        amount: row.amount,
        payeeText: row.payeeText,
        resolvedTransferAccountId: row.resolvedTransferAccountId,
        externalTransferLike: row.externalTransferLike,
        resolvedPayeeId: row.resolvedPayeeId,
        resolvedCategoryId: row.resolvedCategoryId,
        memo: row.memo,
        accepted: options?.accepted ?? row.accepted,
        ignored: options?.ignored ?? row.ignored,
        applyDuplicateResolution: options?.applyDuplicateResolution,
        duplicateConfirmed: options?.duplicateConfirmed,
        duplicateRejected: options?.duplicateRejected,
      })
      .subscribe({
        next: updatedRow => {
          if (!this.importDraft) {
            return;
          }
          const preparedRow = this.prepareImportRow(updatedRow);
          console.log('[txn-import-ui] updateRow:next', {
            rowId: preparedRow.id,
            duplicateSuspected: preparedRow.duplicateSuspected,
            duplicateConfirmed: preparedRow.duplicateConfirmed,
            accepted: preparedRow.accepted,
            ignored: preparedRow.ignored,
          });
          this.importDraft = {
            ...this.importDraft,
            rows: this.importDraft.rows.map(existing =>
              existing.id === preparedRow.id
                ? { ...preparedRow, transferDisplayMode: existing.transferDisplayMode ?? preparedRow.transferDisplayMode }
                : existing,
            ),
          };
          this.recalculateImportDraftSummary();
          this.importBusy = false;
          this.changeDetectorRef.detectChanges();
          this.loadImportHistory();
        },
        error: (error: HttpErrorResponse) => {
          this.importBusy = false;
          this.importError = this.getImportErrorMessage(error);
        },
      });
  }

  commitImport(): void {
    if (!this.importDraft) {
      return;
    }

    const unhandledReviewRows = this.getUnhandledCommitRows(this.importDraft.rows);
    let autoAcceptUnhandled = false;
    if (unhandledReviewRows.length > 0) {
      const confirmed = window.confirm(
        unhandledReviewRows.length === 1
          ? '1 row has not been explicitly accepted or skipped yet. Accept it automatically and continue with commit?'
          : `${unhandledReviewRows.length} rows have not been explicitly accepted or skipped yet. Accept them automatically and continue with commit?`,
      );
      if (!confirmed) {
        this.importError = 'Commit cancelled. Review the remaining rows, or confirm automatic acceptance when you are ready.';
        console.warn('[txn-import-ui] commitImport:cancelled-unhandled-review', {
          importId: this.importDraft.importId,
          rowIds: unhandledReviewRows.map(row => row.id),
        });
        this.changeDetectorRef.detectChanges();
        return;
      }
      autoAcceptUnhandled = true;
    }

    this.importBusy = true;
    this.importError = null;
    console.log('[txn-import-ui] commitImport:start', { importId: this.importDraft.importId, autoAcceptUnhandled });
    this.transactionImportService.commitImport(this.importDraft.importId, { autoAcceptUnhandled }).subscribe({
      next: response => {
        console.log('[txn-import-ui] commitImport:success', {
          importId: response.importId,
          focusTransactionId: response.focusTransactionId,
          focusRowIndex: response.focusRowIndex,
        });
        this.resetImportDialogState({ discardDraft: false, closeDialog: true });
        this.closeImportDialog();
        if (this.accountId) {
          this.refreshTransactionsAfterImportChange();
          this.focusImportedTransactions(response.focusTransactionId ?? null, response.focusRowIndex ?? null);
        }
      },
      error: (error: HttpErrorResponse) => {
        console.error('[txn-import-ui] commitImport:error', error);
        this.importBusy = false;
        this.importError = this.getImportErrorMessage(error);
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  backOutImport(importItem: TransactionImportHistoryItem): void {
    this.importBusy = true;
    this.importError = null;
    this.transactionImportService.backOutImport(importItem.importId).subscribe({
      next: () => {
        this.importBusy = false;
        this.refreshTransactionsAfterImportChange();
        this.loadImportHistory();
      },
      error: (error: HttpErrorResponse) => {
        this.importBusy = false;
        this.importError = this.getImportErrorMessage(error);
      },
    });
  }

  searchPayees(event: AutoCompleteCompleteEvent, row: TransactionImportRow): void {
    const query = event.query?.trim() ?? '';
    const selectedPayee = this.getSelectedImportPayeeOption(row);
    if (!query) {
      this.payeeSuggestions[row.id] = selectedPayee ? [selectedPayee] : [];
      return;
    }

    this.transactionService.getPayeeOptions({ page: 0, size: TransactionsComponent.IMPORT_LOOKUP_PAGE_SIZE, query }).subscribe({
      next: response => {
        this.payeeSuggestions[row.id] = this.mergeSelectedSuggestion(response.body ?? [], selectedPayee);
      },
      error: () => {
        this.payeeSuggestions[row.id] = selectedPayee ? [selectedPayee] : [];
      },
    });
  }

  searchImportTransferAccounts(event: AutoCompleteCompleteEvent, row: TransactionImportRow): void {
    const query = event.query?.trim().toLowerCase() ?? '';
    const selectedTransferAccount = this.getSelectedTransferAccountOption(row);
    const options = this.transferAccountOptions.filter(option => option.name.toLowerCase().includes(query));
    this.importTransferAccountSuggestions[row.id] = this.mergeSelectedSuggestion(options, selectedTransferAccount);
  }

  onImportPayeeSelected(row: TransactionImportRow): void {
    const selectedPayee = this.getSelectedImportPayeeOption(row);
    row.resolvedPayeeId = selectedPayee?.id ?? null;
    row.resolvedPayeeName = selectedPayee?.name ?? null;
    row.payeeText = this.normalizeImportPayeeValue(row.payeeControl ?? null).name;
    row.payeeNeedsReview = !selectedPayee?.id;
    row.reviewMessage = null;
    this.maybePrefillImportCategoryFromPayee(row);
  }

  onImportTransferAccountSelected(row: TransactionImportRow): void {
    const selectedTransferAccount = this.getSelectedTransferAccountOption(row);
    row.resolvedTransferAccountId = selectedTransferAccount?.id ?? null;
    row.resolvedTransferAccountName = selectedTransferAccount?.name ?? null;
    row.externalTransferLike = false;
    row.transferNeedsReview = !selectedTransferAccount?.id;
    row.reviewMessage = null;
    row.resolvedCategoryId = null;
    row.resolvedCategoryName = null;
    row.categoryControl = null;
  }

  clearImportPayee(row: TransactionImportRow): void {
    row.payeeControl = null;
    row.resolvedPayeeId = null;
    row.resolvedPayeeName = null;
    row.payeeText = null;
    row.payeeNeedsReview = true;
    row.reviewMessage = null;
  }

  clearImportTransferAccount(row: TransactionImportRow): void {
    row.transferAccountControl = null;
    row.resolvedTransferAccountId = null;
    row.resolvedTransferAccountName = null;
    row.transferNeedsReview = !row.externalTransferLike;
    row.reviewMessage = null;
  }

  treatTransferAsNormalTransaction(row: TransactionImportRow): void {
    row.transferDisplayMode = false;
    row.externalTransferLike = true;
    row.transferNeedsReview = false;
    row.transferAccountControl = null;
    row.resolvedTransferAccountId = null;
    row.resolvedTransferAccountName = null;
    row.reviewMessage = null;
    this.changeDetectorRef.detectChanges();
    this.saveImportRowWithOptions(row, { accepted: row.accepted, ignored: row.ignored });
  }

  matchTransferToInternalAccount(row: TransactionImportRow): void {
    this.prepareRowForTransferReview(row);
    row.transferDisplayMode = true;
    row.externalTransferLike = false;
    row.transferNeedsReview = !row.resolvedTransferAccountId;
    row.reviewMessage = null;
    this.changeDetectorRef.detectChanges();
  }

  clearImportCategory(row: TransactionImportRow): void {
    row.categoryControl = null;
    row.resolvedCategoryId = null;
    row.resolvedCategoryName = null;
    row.categoryNeedsReview = true;
    row.reviewMessage = null;
  }

  onImportCategoryChanged(row: TransactionImportRow): void {
    row.resolvedCategoryId = this.normalizeImportCategoryValue(row.categoryControl ?? null);
    row.resolvedCategoryName = row.categoryControl ? this.getCategoryTreeSelectLabel(row.categoryControl, row.categoryControl.label) : null;
    row.categoryNeedsReview = !row.resolvedCategoryId;
    row.reviewMessage = null;
  }

  showNewPayeeMessage(row: TransactionImportRow): boolean {
    const payeeName = this.normalizeImportPayeeValue(row.payeeControl ?? row.payeeText ?? null).name;
    return Boolean(row.payeeNeedsReview && payeeName);
  }

  showCategoryRequiredMessage(row: TransactionImportRow): boolean {
    if (this.isPendingInternalTransferRow(row)) {
      return false;
    }
    return !row.resolvedCategoryId;
  }

  showCategoryCheckMessage(row: TransactionImportRow): boolean {
    if (this.isPendingInternalTransferRow(row)) {
      return false;
    }
    return Boolean(row.resolvedCategoryId && row.categoryNeedsReview);
  }

  rowNeedsAttention(row: TransactionImportRow): boolean {
    if (row.duplicateConfirmed || row.ignored) {
      return false;
    }
    return (
      row.transferNeedsReview ||
      row.payeeNeedsReview ||
      row.categoryNeedsReview ||
      row.dateNeedsReview ||
      row.amountNeedsReview ||
      row.balanceMismatch ||
      row.duplicateSuspected
    );
  }

  getImportRowState(row: TransactionImportRow): 'ok' | 'warning' | 'duplicate' | 'transfer' {
    if (row.duplicateConfirmed) {
      return 'ok';
    }
    if (row.duplicateSuspected) {
      return 'duplicate';
    }
    if (this.isPendingInternalTransferRow(row)) {
      return row.transferNeedsReview ? 'warning' : 'transfer';
    }
    return this.rowNeedsAttention(row) ? 'warning' : 'ok';
  }

  getImportRowIcon(row: TransactionImportRow): string {
    switch (this.getImportRowState(row)) {
      case 'duplicate':
        return 'circle-exclamation';
      case 'warning':
        return 'exclamation';
      case 'transfer':
        return 'exchange-alt';
      default:
        return 'check';
    }
  }

  getImportRowLabel(row: TransactionImportRow): string {
    if (row.ignored) {
      return 'Ignored';
    }
    if (row.duplicateConfirmed) {
      return 'Duplicate confirmed';
    }
    if (this.isPendingInternalTransferRow(row)) {
      return row.transferNeedsReview ? 'Transfer needs review' : 'Transfer ready';
    }
    switch (this.getImportRowState(row)) {
      case 'duplicate':
        return 'Possible duplicate';
      case 'warning':
        return 'Needs review';
      case 'transfer':
        return 'Transfer';
      default:
        return 'Ready';
    }
  }

  formatImportWarnings(row: TransactionImportRow): string {
    const warnings: string[] = [];
    if (row.ignored) {
      warnings.push('Ignored');
      return warnings.join(', ');
    }
    if (row.duplicateConfirmed) {
      warnings.push('Confirmed duplicate');
      return warnings.join(', ');
    }
    if (row.payeeNeedsReview) {
      warnings.push('Payee');
    }
    if (row.categoryNeedsReview) {
      warnings.push('Category');
    }
    if (row.transferNeedsReview) {
      warnings.push('Transfer account');
    }
    if (row.dateNeedsReview) {
      warnings.push('Date');
    }
    if (row.amountNeedsReview) {
      warnings.push('Amount');
    }
    if (row.balanceMismatch) {
      warnings.push('Balance');
    }
    if (row.duplicateSuspected) {
      warnings.push('Possible duplicate');
    }
    return warnings.join(', ');
  }

  getImportPayeeLabel(row: TransactionImportRow): string {
    return (row.amount ?? 0) >= 0 ? 'To' : 'Pay to';
  }

  private prepareRowForTransferReview(row: TransactionImportRow): void {
    const payeeName = this.normalizeImportPayeeValue(row.payeeControl ?? row.payeeText ?? null).name;
    const memo = this.trimToNull(row.memo);

    if (payeeName && memo && this.looksLikeGenericTransferReference(payeeName) && !this.looksLikeGenericTransferReference(memo)) {
      row.payeeControl = memo;
      row.payeeText = memo;
      row.memo = payeeName;
      row.resolvedPayeeId = null;
      row.resolvedPayeeName = null;
      row.payeeNeedsReview = true;
    }
  }

  private looksLikeGenericTransferReference(value: string | null): boolean {
    const normalized = this.trimToNull(value)?.toLowerCase();
    if (!normalized) {
      return false;
    }

    return ['transfer', 'funds transfer', 'bank transfer', 'internet transfer', 'payment', 'payment transfer', 'account transfer'].includes(
      normalized,
    );
  }

  formatDuplicateTarget(row: TransactionImportRow): string | null {
    if (!row.duplicateSuspected) {
      return null;
    }

    const parts = [
      row.duplicateTransactionDate ? new Date(row.duplicateTransactionDate).toLocaleDateString() : null,
      row.duplicateTransactionPayeeName?.trim() || row.resolvedPayeeName?.trim() || row.payeeText?.trim() || null,
      row.duplicateTransactionAmount != null ? this.formatAmount(row.duplicateTransactionAmount) : null,
    ].filter((value): value is string => !!value);

    return parts.length ? parts.join(' • ') : 'Matches an existing transaction';
  }

  getVisibleImportRows(rows: TransactionImportRow[]): TransactionImportRow[] {
    return rows.filter(row => {
      if (row.duplicateConfirmed && !this.showConfirmedDuplicateRows) {
        return false;
      }
      if (this.isAcceptedReviewRow(row) && !this.showAcceptedRows) {
        return false;
      }
      if (row.ignored && !this.showIgnoredRows) {
        return false;
      }
      return true;
    });
  }

  countHiddenConfirmedDuplicateRows(rows: TransactionImportRow[]): number {
    return rows.filter(row => row.duplicateConfirmed).length;
  }

  countHiddenDuplicateRows(rows: TransactionImportRow[]): number {
    return 0;
  }

  countHiddenAcceptedRows(rows: TransactionImportRow[]): number {
    return rows.filter(row => this.isAcceptedReviewRow(row)).length;
  }

  countHiddenIgnoredRows(rows: TransactionImportRow[]): number {
    return rows.filter(row => row.ignored).length;
  }

  isAcceptedReviewRow(row: TransactionImportRow): boolean {
    return row.accepted && !row.ignored && !row.duplicateConfirmed;
  }

  private getUnhandledCommitRows(rows: TransactionImportRow[]): TransactionImportRow[] {
    return rows.filter(row => {
      if (row.ignored || row.duplicateConfirmed || row.duplicateSuspected) {
        return false;
      }
      return !row.accepted;
    });
  }

  hasOutstandingWarnings(row: TransactionImportRow): boolean {
    return (
      row.payeeNeedsReview ||
      row.categoryNeedsReview ||
      row.dateNeedsReview ||
      row.amountNeedsReview ||
      row.balanceMismatch ||
      row.duplicateSuspected
    );
  }

  private formatAmount(amount: number): string {
    return formatCurrency(amount, 'en-AU', this.account?.currencyCode ?? '$', this.account?.currencyCode ?? 'AUD');
  }

  private getMissingRequiredReviewFields(row: TransactionImportRow): string[] {
    const missingFields: string[] = [];
    if (this.isPendingInternalTransferRow(row)) {
      if (!row.resolvedTransferAccountId) {
        missingFields.push('transfer account');
      }
      return missingFields;
    }
    const payeeName = this.normalizeImportPayeeValue(row.payeeControl ?? row.payeeText ?? null).name;
    if (!payeeName) {
      missingFields.push('payee');
    }
    if (!row.resolvedCategoryId) {
      missingFields.push('category');
    }
    return missingFields;
  }

  getCategoryTreeSelectLabel(
    value: TransactionTreeOption | TransactionTreeOption[] | null | undefined,
    placeholder = 'No category',
  ): string {
    const selectedValue = Array.isArray(value) ? value[0] : value;
    if (!selectedValue?.key) {
      return placeholder;
    }

    return this.getCategoryPathByKey(selectedValue.key) ?? selectedValue.label;
  }

  positionTreeSelectPanelAbove(event: OverlayOnShowEvent): void {
    requestAnimationFrame(() => {
      const overlay = event.overlay;
      const target = event.target;
      if (!overlay || !target) {
        return;
      }

      const targetRect = target.getBoundingClientRect();
      const overlayRect = overlay.getBoundingClientRect();
      const viewportPadding = 8;
      const gutter = 4;
      const nextTop = Math.max(viewportPadding, targetRect.top + window.scrollY - overlayRect.height - gutter);
      const nextLeft = Math.min(
        Math.max(viewportPadding, targetRect.left + window.scrollX),
        window.scrollX + window.innerWidth - overlayRect.width - viewportPadding,
      );

      overlay.style.top = `${nextTop}px`;
      overlay.style.left = `${nextLeft}px`;
      overlay.style.insetInlineStart = `${nextLeft}px`;
      overlay.style.marginTop = '0';
      overlay.style.transformOrigin = 'bottom';
    });
  }

  openDuplicatePreview(row: TransactionImportRow): void {
    if (!this.accountId || !row.duplicateTransactionId) {
      return;
    }

    this.duplicatePreviewVisible = true;
    this.duplicatePreviewLoading = true;
    this.duplicatePreviewTransaction = null;
    this.duplicatePreviewRow = row;
    this.transactionService.getById(this.accountId, row.duplicateTransactionId).subscribe({
      next: transaction => {
        this.duplicatePreviewTransaction = this.processTransaction({ ...transaction });
        this.duplicatePreviewLoading = false;
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        console.error('[txn-import-ui] duplicatePreview:error', { transactionId: row.duplicateTransactionId, error });
        this.duplicatePreviewLoading = false;
        this.importError = this.getImportErrorMessage(error);
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  closeDuplicatePreview(): void {
    this.duplicatePreviewVisible = false;
    this.duplicatePreviewLoading = false;
    this.duplicatePreviewTransaction = null;
    this.duplicatePreviewRow = null;
  }

  private reloadImportDraft(): void {
    if (!this.importDraft) {
      return;
    }
    this.transactionImportService.getImport(this.importDraft.importId).subscribe({
      next: draft => {
        console.log('[txn-import-ui] reloadImportDraft:next', { importId: draft.importId, rows: draft.rows?.length ?? 0 });
        this.importDraft = this.prepareImportDraft(draft);
        this.importBusy = false;
        this.changeDetectorRef.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        console.error('[txn-import-ui] reloadImportDraft:error', error);
        this.importBusy = false;
        this.importError = this.getImportErrorMessage(error);
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  private recalculateImportDraftSummary(): void {
    if (!this.importDraft) {
      return;
    }

    const flaggedRows = this.importDraft.rows.filter(row => this.rowNeedsAttention(row)).length;
    this.importDraft = {
      ...this.importDraft,
      totalRows: this.importDraft.rows.length,
      flaggedRows,
    };
  }

  private loadImportHistory(): void {
    if (!this.accountId) {
      return;
    }
    this.importHistoryLoading = true;
    this.transactionImportService.getAccountImports(this.accountId).subscribe({
      next: history => {
        console.log('[txn-import-ui] loadImportHistory:next', { count: history.length, importIds: history.map(item => item.importId) });
        this.importHistory = history.map(item => ({
          ...item,
          createdDateTime: this.normalizeImportDateTime(item.createdDateTime),
        }));
        this.importHistoryLoading = false;
        this.changeDetectorRef.detectChanges();
      },
      error: () => {
        console.error('[txn-import-ui] loadImportHistory:error');
        this.importHistory = [];
        this.importHistoryLoading = false;
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  private getImportErrorMessage(error: HttpErrorResponse): string {
    const message =
      typeof error.error === 'string'
        ? error.error
        : typeof error.error?.message === 'string'
          ? error.error.message
          : typeof error.error?.detail === 'string'
            ? error.error.detail
            : null;
    return message ?? `Import request failed (${error.status || 'unknown error'}).`;
  }

  private normalizeExpectedEndingBalance(value: string | number | null): number | null {
    if (typeof value === 'number') {
      return Number.isNaN(value) ? null : value;
    }

    if (typeof value === 'string') {
      const trimmedValue = value.trim();
      if (!trimmedValue) {
        return null;
      }
      const numericValue = Number(trimmedValue);
      return Number.isNaN(numericValue) ? null : numericValue;
    }

    return null;
  }

  private discardDraftImport(importId: string): void {
    this.transactionImportService.discardImport(importId).subscribe({
      next: () => {
        console.log('[txn-import-ui] discardDraft:success', { importId });
        this.loadImportHistory();
      },
      error: error => {
        console.error('[txn-import-ui] discardDraft:error', { importId, error });
      },
    });
  }

  private resetImportDialogState(options: { discardDraft: boolean; closeDialog: boolean }): void {
    const importIdToDiscard = options.discardDraft && this.importDraft?.status === 'DRAFT' ? this.importDraft.importId : null;
    this.importDialogVisible = !options.closeDialog && this.importDialogVisible;
    this.importDialogTab = 'new';
    this.importError = null;
    this.importBusy = false;
    this.stopImportProgress();
    this.importDraft = null;
    this.importRawContent = '';
    this.importRawHtml = null;
    this.expectedEndingBalance = '';
    this.importHistory = [];
    this.importHistoryLoading = false;
    this.payeeSuggestions = {};
    this.importTransferAccountSuggestions = {};
    this.importCategoryOptions = [];
    this.importCategoryLoading = false;
    this.duplicatePreviewVisible = false;
    this.duplicatePreviewLoading = false;
    this.duplicatePreviewTransaction = null;
    this.duplicatePreviewRow = null;
    this.showDuplicateRows = false;
    this.showConfirmedDuplicateRows = false;
    this.showAcceptedRows = false;
    this.showIgnoredRows = false;
    this.importStateAccountId = options.closeDialog ? null : this.accountId;
    this.changeDetectorRef.detectChanges();
    if (importIdToDiscard) {
      this.discardDraftImport(importIdToDiscard);
    }
  }

  private startImportProgress(): void {
    this.stopImportProgress();
    this.importProgressMessageIndex = 0;
    this.importProgressTimer = setInterval(() => {
      this.ngZone.run(() => {
        this.importProgressMessageIndex = (this.importProgressMessageIndex + 1) % this.importProgressMessages.length;
        this.changeDetectorRef.detectChanges();
      });
    }, 1800);
  }

  private stopImportProgress(): void {
    if (this.importProgressTimer) {
      clearInterval(this.importProgressTimer);
      this.importProgressTimer = null;
    }
    this.importProgressMessageIndex = 0;
  }

  private applyImportDraft(source: 'post-response', draft: TransactionImportDraft): void {
    this.ngZone.run(() => {
      if (this.accountId && draft.accountId !== this.accountId) {
        console.warn('[txn-import-ui] applyImportDraft:ignored-account-mismatch', {
          source,
          currentAccountId: this.accountId,
          draftAccountId: draft.accountId,
          importId: draft.importId,
        });
        this.importBusy = false;
        this.stopImportProgress();
        this.discardDraftImport(draft.importId);
        this.changeDetectorRef.detectChanges();
        return;
      }
      console.log('[txn-import-ui] applyImportDraft', {
        source,
        importId: draft.importId,
        rows: draft.rows?.length ?? 0,
        flaggedRows: draft.flaggedRows,
      });
      this.importStateAccountId = draft.accountId;
      this.importDraft = this.prepareImportDraft(draft);
      this.importDialogTab = 'new';
      this.importBusy = false;
      this.stopImportProgress();
      this.changeDetectorRef.detectChanges();
    });
  }

  private prepareImportDraft(draft: TransactionImportDraft): TransactionImportDraft {
    return {
      ...draft,
      createdDateTime: this.normalizeImportDateTime(draft.createdDateTime),
      rows: draft.rows.map(row => this.prepareImportRow(row)),
    };
  }

  private prepareImportRow(row: TransactionImportRow): TransactionImportRow {
    const selectedPayee = row.resolvedPayeeName ? { id: row.resolvedPayeeId ?? row.id, name: row.resolvedPayeeName } : null;
    const selectedTransferAccount =
      row.resolvedTransferAccountId && row.resolvedTransferAccountName
        ? { id: row.resolvedTransferAccountId, name: row.resolvedTransferAccountName }
        : null;
    const selectedCategory =
      row.resolvedCategoryId && row.resolvedCategoryName
        ? (this.findCategoryOptionByKey(row.resolvedCategoryId) ?? {
            key: row.resolvedCategoryId,
            label: row.resolvedCategoryName,
            leaf: true,
          })
        : null;

    return {
      ...row,
      duplicateStrongMatch: Boolean(row.duplicateStrongMatch),
      duplicateRejected: Boolean(row.duplicateRejected),
      reviewMessage: null,
      payeeControl: selectedPayee ?? row.payeeText ?? null,
      categoryControl: selectedCategory,
      transferAccountControl: selectedTransferAccount,
      transferDisplayMode: row.transferDisplayMode ?? this.isTransferRow(row),
    };
  }

  private loadImportCategoryTreeOptions(): void {
    this.importCategoryLoading = true;
    this.transactionService.getCategoryTreeOptions().subscribe({
      next: options => {
        this.importCategoryLoading = false;
        this.importCategoryOptions = options;
        if (this.importDraft) {
          this.importDraft = this.prepareImportDraft(this.importDraft);
          this.changeDetectorRef.detectChanges();
        }
      },
      error: () => {
        this.importCategoryLoading = false;
        this.importCategoryOptions = [];
      },
    });
  }

  private getSelectedImportPayeeOption(row: TransactionImportRow): TransactionOption | null {
    const payee = row.payeeControl;
    if (!payee || typeof payee === 'string') {
      return null;
    }

    return payee;
  }

  private getSelectedTransferAccountOption(row: TransactionImportRow): TransactionOption | null {
    const transferAccount = row.transferAccountControl;
    if (!transferAccount) {
      return null;
    }

    return transferAccount;
  }

  private normalizeImportPayeeValue(payee: TransactionOption | string | null): { id: string | null; name: string | null } {
    if (payee == null) {
      return { id: null, name: null };
    }

    if (typeof payee === 'string') {
      const normalizedName = this.trimToNull(payee);
      return { id: null, name: normalizedName };
    }

    return {
      id: payee.id,
      name: this.trimToNull(payee.name),
    };
  }

  private normalizeTransferAccountValue(account: TransactionOption | null): { id: string | null; name: string | null } {
    if (!account) {
      return { id: null, name: null };
    }

    return {
      id: account.id,
      name: this.trimToNull(account.name),
    };
  }

  private maybePrefillImportCategoryFromPayee(row: TransactionImportRow): void {
    const payee = this.getSelectedImportPayeeOption(row);
    if (!payee?.id) {
      return;
    }

    const currentCategoryKey = this.normalizeImportCategoryValue(row.categoryControl ?? null);
    if (currentCategoryKey && currentCategoryKey !== row.resolvedCategoryId) {
      return;
    }

    this.transactionService.getLastCategoryForPayee(payee.id, (row.amount ?? 0) >= 0 ? 'deposit' : 'withdrawal').subscribe({
      next: category => {
        if (!category?.id) {
          return;
        }

        row.resolvedCategoryId = category.id;
        row.resolvedCategoryName = category.name;
        row.categoryControl = this.findCategoryOptionByKey(category.id) ?? { key: category.id, label: category.name, leaf: true };
        this.changeDetectorRef.detectChanges();
      },
      error: () => {},
    });
  }

  private normalizeImportCategoryValue(category: TransactionTreeOption | null): string | null {
    if (category == null) {
      return null;
    }

    return this.trimToNull(category.key);
  }

  private mergeSelectedSuggestion(options: TransactionOption[], selectedOption: TransactionOption | null): TransactionOption[] {
    if (!selectedOption) {
      return options;
    }

    if (options.some(option => option.id === selectedOption.id)) {
      return options;
    }

    return [...options, selectedOption];
  }

  isTransferRow(row: TransactionImportRow): boolean {
    return (
      row.transactionKind === 'TRANSFER' ||
      Boolean(this.trimToNull(row.transferAccountText)) ||
      Boolean(row.resolvedTransferAccountId) ||
      Boolean(this.trimToNull(row.resolvedTransferAccountName)) ||
      row.transferNeedsReview ||
      row.externalTransferLike
    );
  }

  isTransferDisplayRow(row: TransactionImportRow): boolean {
    return row.transferDisplayMode ?? this.isTransferRow(row);
  }

  isPendingInternalTransferRow(row: TransactionImportRow): boolean {
    return this.isTransferDisplayRow(row);
  }

  getTransferReviewMessage(row: TransactionImportRow): string | null {
    if (!this.isTransferRow(row)) {
      return null;
    }
    if (row.externalTransferLike) {
      return 'Looks like transfer to external account - review as normal transaction';
    }
    if (row.resolvedTransferAccountName) {
      return `Matched internal transfer account: ${row.resolvedTransferAccountName}`;
    }
    return 'Possible internal transfer - please confirm';
  }

  getTransferReviewTooltip(row: TransactionImportRow): string | null {
    if (!this.isTransferRow(row)) {
      return null;
    }
    if (row.externalTransferLike || !row.resolvedTransferAccountName) {
      return this.getTransferReviewMessage(row);
    }
    return null;
  }

  getTransferConvertActionLabel(row: TransactionImportRow): string {
    return (row.amount ?? 0) >= 0 ? 'Convert to Deposit' : 'Convert to Withdrawal';
  }

  private findCategoryOptionByKey(key: string): TransactionTreeOption | null {
    return this.findTreeOptionByKeyInNodes(this.importCategoryOptions, key);
  }

  private findTreeOptionByKeyInNodes(nodes: TransactionTreeOption[], key: string): TransactionTreeOption | null {
    for (const node of nodes) {
      if (node.key === key) {
        return node;
      }
      const childMatch = this.findTreeOptionByKeyInNodes(node.children ?? [], key);
      if (childMatch) {
        return childMatch;
      }
    }

    return null;
  }

  private getCategoryPathByKey(key: string): string | null {
    const path = this.findCategoryPathByKey(this.importCategoryOptions, key);
    if (!path?.length) {
      return null;
    }

    const categoryPath = path.filter(label => !['income', 'expense'].includes(label.toLowerCase()));
    return categoryPath.length ? categoryPath.join(' : ') : path[path.length - 1];
  }

  private findCategoryPathByKey(nodes: TransactionTreeOption[], key: string, trail: string[] = []): string[] | null {
    for (const node of nodes) {
      const nextTrail = [...trail, node.label];
      if (node.key === key) {
        return nextTrail;
      }
      const childPath = this.findCategoryPathByKey(node.children ?? [], key, nextTrail);
      if (childPath) {
        return childPath;
      }
    }

    return null;
  }

  private trimToNull(value: string | null): string | null {
    if (value == null) {
      return null;
    }

    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private normalizeImportDateTime(value: string | null): string {
    const normalizedValue = this.trimToNull(value);
    if (!normalizedValue) {
      return '';
    }

    // Java ZonedDateTime strings append the region in brackets, which Angular DatePipe won't parse.
    return normalizedValue.replace(/\[[^\]]+\]$/, '');
  }

  private buildColumnDefs(): (ColDef | ColGroupDef)[] {
    const floatingFilter = this.showFilterRow;

    return [
      {
        colId: 'date',
        field: 'date',
        headerName: 'Date',
        sort: 'desc',
        filter: 'agDateColumnFilter',
        floatingFilter,
        hide: !this.isColumnVisible('date'),
      },
      {
        colId: 'payee',
        field: 'payeeName',
        headerName: 'Payee',
        filter: 'agTextColumnFilter',
        floatingFilter,
        hide: !this.isColumnVisible('payee'),
        valueGetter: params => this.getDisplayPayeeName(params.data ?? null),
      },
      {
        colId: 'security',
        field: 'securityName',
        headerName: 'Security',
        hide: !this.isColumnVisible('security'),
        filter: 'agTextColumnFilter',
        floatingFilter,
        cellClass: params => (params.data?.securityId ? 'transactions-grid__security-link' : ''),
        valueGetter: params => this.getDisplaySecurityName(params.data ?? null),
      },
      {
        colId: 'memo',
        field: 'memo',
        headerName: 'Memo',
        hide: !this.isColumnVisible('memo'),
        filter: 'agTextColumnFilter',
        floatingFilter,
      },
      {
        colId: 'who',
        field: 'whoName',
        headerName: 'Who',
        hide: !this.isColumnVisible('who'),
        filter: 'agTextColumnFilter',
        floatingFilter,
      },
      {
        colId: 'amount',
        field: 'amount',
        headerName: 'Amount',
        hide: !this.isColumnVisible('amount'),
        filter: 'agNumberColumnFilter',
        floatingFilter,
      },
      {
        colId: 'category',
        field: 'displayCategory',
        headerName: 'Category',
        sortable: true,
        hide: !this.isColumnVisible('category'),
        filter: 'agTextColumnFilter',
        floatingFilter,
      },
      {
        colId: 'tags',
        field: 'tagsDisplay',
        headerName: 'Tags',
        sortable: true,
        hide: !this.isColumnVisible('tags'),
        filter: 'agTextColumnFilter',
        floatingFilter,
      },
      {
        colId: 'payment',
        field: 'payment',
        headerName: 'Payment',
        type: 'numericColumn',
        sortable: true,
        hide: !this.isColumnVisible('payment'),
        filter: 'agNumberColumnFilter',
        floatingFilter,
        valueFormatter(params) {
          if (!params.value) return '';
          return formatCurrency(params.value, 'en-AU', '$', 'AUD');
        },
      },
      {
        colId: 'deposit',
        field: 'deposit',
        headerName: 'Deposit',
        type: 'numericColumn',
        sortable: true,
        hide: !this.isColumnVisible('deposit'),
        filter: 'agNumberColumnFilter',
        floatingFilter,
        valueFormatter(params) {
          if (!params.value) return '';
          return formatCurrency(params.value, 'en-AU', '$', 'AUD');
        },
      },
      {
        colId: 'runningBalance',
        field: 'runningBalance',
        headerName: 'Balance',
        type: 'numericColumn',
        sortable: true,
        hide: !this.isColumnVisible('runningBalance'),
        filter: 'agNumberColumnFilter',
        floatingFilter,
        valueFormatter(params) {
          if (!params.value) return '';
          return formatCurrency(params.value, 'en-AU', '$', 'AUD');
        },
      },
    ];
  }

  private buildTransferAccountOptions(): TransactionOption[] {
    return (this.accounts ?? [])
      .filter(account => account.id !== this.accountId)
      .map(account => ({
        id: account.id,
        name: account.name,
      }));
  }

  private getDisplayPayeeName(transaction: FinancialTransaction | null): string {
    if (!transaction) {
      return '';
    }

    const storedPayeeName = this.trimToNull(transaction.payeeName ?? null);
    const derivedDescriptor = this.getTransferPayeeDescriptor(transaction);
    if (!transaction.transferredAccountId && storedPayeeName) {
      return transaction.payeeName ?? '';
    }

    if (derivedDescriptor) {
      return derivedDescriptor;
    }

    const transferredAccountName =
      this.accounts?.find(account => account.id === transaction.transferredAccountId)?.name ?? transaction.transferredAccountId;
    const directionLabel = transaction.amount < 0 ? 'Transfer to' : 'Transfer from';
    const payeeSuffix = storedPayeeName ? ` (${storedPayeeName})` : '';

    return `${directionLabel}: ${transferredAccountName}${payeeSuffix}`;
  }

  private getTransferPayeeDescriptor(transaction: FinancialTransaction): string | null {
    const categoryLabel = this.trimToNull(transaction.categoryName ?? transaction.displayCategory ?? null);
    const securityLabel = this.trimToNull(transaction.securityName ?? transaction.securityId ?? null);
    if (!categoryLabel && !securityLabel) {
      return null;
    }

    return [categoryLabel, securityLabel].filter((value): value is string => !!value).join(': ');
  }

  private getDisplaySecurityName(transaction: FinancialTransaction | null): string {
    if (!transaction?.securityId) {
      return '';
    }

    return transaction.securityName ?? transaction.securityId;
  }

  private buildAccountSelectOptions(): AccountSelectGroup[] {
    const accounts = this.accounts ?? [];
    const selectedAccountId = this.accountId;
    const visibleAccounts = accounts.filter(account => !account.closed || this.showClosedAccounts || account.id === selectedAccountId);
    const favourites = visibleAccounts.filter(account => account.favourite);
    const openAccounts = visibleAccounts.filter(account => !account.favourite && !account.closed);
    const closedAccounts = visibleAccounts.filter(account => !account.favourite && account.closed);
    const groups: AccountSelectGroup[] = [];

    if (favourites.length > 0) {
      groups.push({ label: 'Favourites', items: favourites });
    }
    if (openAccounts.length > 0) {
      groups.push({ label: 'Open accounts', items: openAccounts });
    }
    if (closedAccounts.length > 0) {
      groups.push({ label: 'Closed accounts', items: closedAccounts });
    }

    return groups;
  }

  private sortAccounts(accounts: FinancialAccount[]): FinancialAccount[] {
    return [...accounts].sort((left, right) => {
      if (left.favourite !== right.favourite) {
        return left.favourite ? -1 : 1;
      }
      if (left.closed !== right.closed) {
        return left.closed ? 1 : -1;
      }
      return left.name.localeCompare(right.name);
    });
  }

  private onSuccess(transactions: FinancialTransaction[] | null, headers: HttpHeaders): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.processTransactions(transactions!);
  }

  private processTransactions(transactions: FinancialTransaction[]): void {
    transactions.forEach(element => this.processTransaction(element));

    this.transactions = transactions;

    if (this.balance === -1 && transactions.length > 0) {
      this.balance = transactions[0].runningBalance;
    } else if (this.balance === -1 && transactions.length === 0 && this.account != null) {
      this.balance = this.account.startingBalance;
    }
  }

  private processTransaction(element: FinancialTransaction): FinancialTransaction {
    element.payment = 0;
    element.deposit = 0;
    element.displayCategory = '';
    element.tagsDisplay = element.tagsDisplay ?? ((element.tags ?? []).join(', ') || '');

    if (element.splitParent) {
      element.displayCategory = 'Split / Multiple Categories';
    } else if (element.categoryName) {
      element.displayCategory = element.categoryName;
      if (element.parentCategoryName) {
        element.displayCategory = `${element.parentCategoryName}: ${element.displayCategory}`;
      }
    }

    const startingBalance = this.account?.startingBalance ?? 0;
    if (startingBalance !== 0) {
      element.runningBalance = element.runningBalance + startingBalance;
    }

    if (element.amount >= 0) {
      element.deposit = element.amount;
    } else {
      element.payment = element.amount;
    }

    return element;
  }

  private createDraftTransaction(request?: TransactionEditorDraftRequest | void): FinancialTransaction {
    const today = new Date();
    const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    const requestedAmount = Math.abs(request?.amount ?? 0);

    return {
      id: '',
      date: request?.date ?? localDate,
      name: '',
      type: 0,
      payeeName: '',
      payeeId: '',
      memo: request?.memo ?? null,
      amount: requestedAmount,
      runningBalance: this.balance !== -1 ? this.balance : (this.account?.startingBalance ?? 0),
      categoryId: '',
      categoryName: '',
      parentCategoryId: '',
      parentCategoryName: '',
      splitParent: false,
      splitChild: false,
      transferredAccountId: '',
      cleared: request?.cleared ?? false,
      voided: false,
      number: 0,
      payment: 0,
      deposit: 0,
      displayCategory: '',
      tags: request?.tags ?? [],
      tagsDisplay: (request?.tags ?? []).join(', '),
      whoId: null,
      whoName: null,
    };
  }

  private buildGridQuery(params: IGetRowsParams): TransactionGridQuery {
    const page = Math.floor(params.startRow / this.itemsPerPage);
    const sort = params.sortModel
      .map(model => this.mapSortModel(model.colId, model.sort))
      .filter((value): value is string => value !== null);

    return {
      page,
      size: this.itemsPerPage,
      sort: sort.length > 0 ? sort : TransactionsComponent.DEFAULT_SORT,
      filters: Object.keys(params.filterModel ?? {}).length > 0 ? JSON.stringify(params.filterModel) : undefined,
    };
  }

  private mapSortModel(columnId: string, direction: 'asc' | 'desc' | null | undefined): string | null {
    if (!direction) {
      return null;
    }

    if (columnId === 'date') {
      return `date,${direction}`;
    }
    if (columnId === 'payee') {
      return `payeeName,${direction}`;
    }
    if (columnId === 'category') {
      return `displayCategory,${direction}`;
    }
    if (columnId === 'memo') {
      return `memo,${direction}`;
    }
    if (columnId === 'amount') {
      return `amount,${direction}`;
    }
    if (columnId === 'payment') {
      return `payment,${direction}`;
    }
    if (columnId === 'deposit') {
      return `deposit,${direction}`;
    }
    if (columnId === 'runningBalance') {
      return `runningBalance,${direction}`;
    }
    if (columnId === 'who') {
      return `whoName,${direction}`;
    }
    if (columnId === 'tags') {
      return `tagsDisplay,${direction}`;
    }

    return null;
  }

  private refreshSelectedRow(transaction: FinancialTransaction): void {
    this.agGrid?.api.forEachNode(node => {
      if (node.data?.id === transaction.id) {
        node.setData({ ...node.data, ...transaction });
      }
    });
  }

  private keepSelectedRowVisible(rowIndex: number | null): void {
    if (rowIndex == null) {
      return;
    }

    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        this.agGrid?.api.ensureIndexVisible(rowIndex, 'middle');
      });
    });
  }

  private focusImportedTransactions(transactionId: string | null, rowIndex: number | null): void {
    if (!transactionId || rowIndex == null || rowIndex < 0) {
      return;
    }

    this.pendingImportGridFocus = { transactionId, rowIndex };
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        this.agGrid?.api.ensureIndexVisible(rowIndex, 'middle');
      });
    });
  }

  private openLinkedTransferTransaction(transaction: FinancialTransaction): void {
    const targetAccountId = transaction.accountId ?? transaction.transferredAccountId ?? null;
    if (!targetAccountId) {
      this.saveTransactionError = 'The linked transfer transaction could not be opened because its account was not available.';
      return;
    }

    this.switchAccount(targetAccountId, { closeEditor: false });
    this.selectedTransactionId = transaction.id;
    this.selectedTransaction = this.processTransaction({ ...transaction });
    this.editorMode = 'view';
    this.editorInitialTransactionType = null;
    this.isEditorOpen = true;
    this.pendingSelectedTransactionFocusId = transaction.id;
    void this.router.navigate(['/transactions', targetAccountId]);
    this.changeDetectorRef.detectChanges();
  }

  private switchAccount(accountId: string, options?: { closeEditor?: boolean }): void {
    const closeEditor = options?.closeEditor ?? true;
    this.balance = -1;
    if (closeEditor) {
      this.closeEditor();
    }
    if (this.importStateAccountId && this.importStateAccountId !== accountId) {
      this.resetImportDialogState({ discardDraft: true, closeDialog: true });
    }
    this.accountId = accountId;
    this.accountSelectOptions = this.buildAccountSelectOptions();
    this.account = this.getAccount(accountId);
    this.transferAccountOptions = this.buildTransferAccountOptions();
    this.agGrid?.api.setGridOption('datasource', this.dataSource);
    this.saveCookie(TransactionsComponent.PREV_TXN_ACCOUNT_ID, accountId);
  }

  private applyPendingSelectedTransactionFocus(): void {
    if (!this.pendingSelectedTransactionFocusId) {
      return;
    }

    const transactionId = this.pendingSelectedTransactionFocusId;
    let focused = false;
    this.agGrid?.api.forEachNode(node => {
      if (focused || node.data?.id !== transactionId) {
        return;
      }
      focused = true;
      node.setSelected(true, true);
      this.keepSelectedRowVisible(node.rowIndex ?? null);
    });

    if (focused) {
      this.pendingSelectedTransactionFocusId = null;
    }
  }

  private applyPendingImportGridFocus(): void {
    if (!this.pendingImportGridFocus) {
      return;
    }

    const focusTarget = this.pendingImportGridFocus;
    let focused = false;
    this.agGrid?.api.forEachNode(node => {
      if (focused || node.data?.id !== focusTarget.transactionId) {
        return;
      }
      focused = true;
      node.setSelected(true, true);
      this.keepSelectedRowVisible(node.rowIndex ?? focusTarget.rowIndex);
    });

    if (focused) {
      this.pendingImportGridFocus = null;
      return;
    }

    requestAnimationFrame(() => {
      this.agGrid?.api.ensureIndexVisible(focusTarget.rowIndex, 'middle');
    });
  }

  private refreshTransactionsAfterImportChange(): void {
    this.balance = -1;
    this.agGrid?.api.refreshInfiniteCache();
  }

  private getSaveTransactionErrorMessage(error: HttpErrorResponse): string {
    if (error.error?.detail) {
      return error.error.detail;
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (error.message) {
      return error.message;
    }

    return 'Saving the transaction failed. Check the browser console for details.';
  }

  private loadColumnVisibility(): void {
    const cookieValue = this.getCookie(TransactionsComponent.COLUMN_VISIBILITY);
    if (!cookieValue) {
      return;
    }

    try {
      const parsedValue = JSON.parse(cookieValue);
      if (!Array.isArray(parsedValue)) {
        return;
      }

      const validColumnIds = new Set(this.columnToggleOptions.map(option => option.id));
      const nextVisibleColumnIds = parsedValue.filter((value): value is string => typeof value === 'string' && validColumnIds.has(value));
      if (nextVisibleColumnIds.length > 0) {
        this.visibleColumnIds = new Set(nextVisibleColumnIds);
      }
    } catch {
      // Ignore invalid cookie data and fall back to defaults.
    }
  }

  private persistColumnVisibility(): void {
    this.saveCookie(TransactionsComponent.COLUMN_VISIBILITY, JSON.stringify(Array.from(this.visibleColumnIds)));
  }

  private refreshColumnDefs(): void {
    this.columnDefs = this.buildColumnDefs();
    this.agGrid?.api.setGridOption('columnDefs', this.columnDefs);
  }

  private getPageTitle(routeSnapshot: ActivatedRouteSnapshot): string {
    let title: string = routeSnapshot.data['pageTitle'] ?? 'App';
    if (routeSnapshot.firstChild) {
      title = this.getPageTitle(routeSnapshot.firstChild) || title;
    }
    return title;
  }

  private getCookie(key: string): string | undefined {
    return this.cookieService.get(key);
  }

  private saveCookie(key: string, value: string): void {
    return this.cookieService.put(key, value);
  }
}

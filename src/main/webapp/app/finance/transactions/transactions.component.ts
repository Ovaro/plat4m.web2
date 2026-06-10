import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild, inject } from '@angular/core';
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
import { TransactionEditorComponent } from './transaction-editor.component';
import { TransactionEditorMode, TransactionGridQuery, TransactionOption, TransactionUpdate } from './transactions.types';

@Component({
  selector: 'jhi-transactions',
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.scss'],
  imports: [SharedModule, NgApexchartsModule, AgGridAngular, TransactionEditorComponent],
})
export class TransactionsComponent implements OnInit {
  static PREV_TXN_ACCOUNT_ID = 'previous-txn-account-id';
  static SHOW_FILTER_ROW = 'transactions-show-filter-row';
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
  showFilterRow = false;
  editorMode: TransactionEditorMode = 'view';
  selectedTransactionId: string | null = null;
  selectedTransaction: FinancialTransaction | null = null;
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
  transferAccountOptions: TransactionOption[] = [];

  @ViewChild(AgGridAngular) agGrid?: AgGridAngular;

  rowModelType: RowModelType = 'infinite';

  gridOptions: GridOptions = {
    pagination: false,
    rowSelection: 'single',
    rowModelType: this.rowModelType,
    cacheBlockSize: this.itemsPerPage,
    paginationPageSize: this.itemsPerPage,
    sideBar: {
      toolPanels: [
        {
          id: 'filters',
          labelDefault: 'Filters',
          labelKey: 'filters',
          iconKey: 'filter',
          toolPanel: 'agFiltersToolPanel',
        },
      ],
      defaultToolPanel: 'filters',
    },
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

  constructor() {
    this.agGridThemeDefinition = this.agGridThemeService.getThemeDefinition(this.agGridTheme, this.theme);
  }

  ngOnInit(): void {
    this.theme = this.themeService.theme();
    this.agGridTheme = this.agGridThemeService.theme();
    this.agGridThemeDefinition = this.agGridThemeService.getThemeDefinition(this.agGridTheme, this.theme);
    this.showFilterRow = this.getCookie(TransactionsComponent.SHOW_FILTER_ROW) === 'true';
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
        this.accounts = res;
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
    this.columnDefs = this.buildColumnDefs();
    this.agGrid?.api.setGridOption('defaultColDef', this.defaultColDef);
    this.agGrid?.api.setGridOption('columnDefs', this.columnDefs);
    this.saveCookie(TransactionsComponent.SHOW_FILTER_ROW, String(this.showFilterRow));
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

  onGridReady(params: GridReadyEvent): void {
    params.api.setGridOption('datasource', this.dataSource);
  }

  onModelUpdated(_event: ModelUpdatedEvent<any>): void {
    this.autoSize();
  }

  onCellClicked(e: CellClickedEvent): void {
    if (!e.data) {
      return;
    }

    this.selectedTransactionId = e.data.id;
    this.selectedTransaction = this.processTransaction({ ...e.data });
    this.editorMode = 'view';
    this.isEditorOpen = true;
    e.node.setSelected(true, true);
    this.keepSelectedRowVisible(e.node.rowIndex ?? null);
  }

  openAddTransaction(): void {
    if (!this.accountId || !this.account) {
      return;
    }

    this.isEditorOpen = true;
    this.editorMode = 'add';
    this.selectedTransactionId = null;
    this.selectedTransaction = this.createDraftTransaction();
    this.agGrid?.api.deselectAll();
  }

  openEditMode(): void {
    if (!this.selectedTransaction || this.getSelectedTransactionReadonlyReason()) {
      return;
    }

    this.editorMode = 'edit';
  }

  closeEditor(): void {
    this.isEditorOpen = false;
    this.editorMode = 'view';
    this.selectedTransactionId = null;
    this.selectedTransaction = null;
    this.agGrid?.api.deselectAll();
  }

  saveTransaction(update: TransactionUpdate): void {
    if (!this.accountId) {
      return;
    }

    this.isSavingTransaction = true;
    const request =
      this.editorMode === 'add' || !this.selectedTransactionId
        ? this.transactionService.create(this.accountId, update)
        : this.transactionService.update(this.accountId, this.selectedTransactionId, update);

    request.subscribe({
      next: transaction => {
        this.isSavingTransaction = false;
        this.editorMode = 'view';
        this.selectedTransactionId = transaction.id;
        this.selectedTransaction = this.processTransaction({ ...transaction });
        this.refreshSelectedRow(this.selectedTransaction);
        this.agGrid?.api.refreshInfiniteCache();
      },
      error: () => {
        this.isSavingTransaction = false;
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
    if (this.selectedTransaction.splitParent || this.selectedTransaction.splitChild) {
      return 'Split transactions are not editable in this panel yet.';
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

  private buildColumnDefs(): (ColDef | ColGroupDef)[] {
    const floatingFilter = this.showFilterRow;

    return [
      { field: 'date', headerName: 'Date', sort: 'desc', filter: 'agDateColumnFilter', floatingFilter },
      {
        colId: 'payee',
        field: 'payeeName',
        headerName: 'Payee',
        colSpan: params => (params.data?.payeeName?.startsWith('Transfer ') ? 2 : 1),
        filter: 'agTextColumnFilter',
        floatingFilter,
      },
      { colId: 'memo', field: 'memo', headerName: 'Memo', hide: true, filter: 'agTextColumnFilter', floatingFilter },
      { colId: 'who', field: 'whoName', headerName: 'Who', hide: true, filter: 'agTextColumnFilter', floatingFilter },
      { colId: 'amount', field: 'amount', headerName: 'Amount', hide: true, filter: 'agNumberColumnFilter', floatingFilter },
      { colId: 'category', field: 'displayCategory', headerName: 'Category', sortable: true, filter: 'agTextColumnFilter', floatingFilter },
      { colId: 'tags', field: 'tagsDisplay', headerName: 'Tags', sortable: true, filter: 'agTextColumnFilter', floatingFilter },
      {
        field: 'payment',
        headerName: 'Payment',
        type: 'numericColumn',
        sortable: true,
        filter: 'agNumberColumnFilter',
        floatingFilter,
        valueFormatter(params) {
          if (!params.value) return '';
          return formatCurrency(params.value, 'en-AU', '$', 'AUD');
        },
      },
      {
        field: 'deposit',
        headerName: 'Deposit',
        type: 'numericColumn',
        sortable: true,
        filter: 'agNumberColumnFilter',
        floatingFilter,
        valueFormatter(params) {
          if (!params.value) return '';
          return formatCurrency(params.value, 'en-AU', '$', 'AUD');
        },
      },
      {
        field: 'runningBalance',
        headerName: 'Balance',
        type: 'numericColumn',
        sortable: true,
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
        element.displayCategory = `${element.displayCategory}: ${element.parentCategoryName}`;
      }
    }

    if (element.transferredAccountId) {
      if (element.amount >= 0) {
        element.payeeName = 'Transfer from : ' + this.getAccountName(element.transferredAccountId);
      } else {
        element.payeeName = 'Transfer to : ' + this.getAccountName(element.transferredAccountId);
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

  private createDraftTransaction(): FinancialTransaction {
    const today = new Date();
    const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    return {
      id: '',
      date: localDate,
      name: '',
      type: 0,
      payeeName: '',
      payeeId: '',
      memo: null,
      amount: 0,
      runningBalance: this.balance !== -1 ? this.balance : (this.account?.startingBalance ?? 0),
      categoryId: '',
      categoryName: '',
      parentCategoryId: '',
      parentCategoryName: '',
      splitParent: false,
      splitChild: false,
      transferredAccountId: '',
      cleared: false,
      voided: false,
      number: 0,
      payment: 0,
      deposit: 0,
      displayCategory: '',
      tags: [],
      tagsDisplay: '',
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

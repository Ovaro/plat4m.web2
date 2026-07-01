import { ChangeDetectorRef, Component, DEFAULT_CURRENCY_CODE, Inject, LOCALE_ID, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { ApexAxisChartSeries, NgApexchartsModule } from 'ng-apexcharts';
import { forkJoin, of } from 'rxjs';

import SharedModule from 'app/shared/shared.module';
import { FinancialAccount, FinancialTransaction } from '../finance.model';
import { AccountList } from '../account-list/account-list.service';
import { FinanceManageDataService } from '../manage-data/finance-manage-data.service';
import { ManagedPayee, ManagedPayeeUpdate } from '../manage-data/finance-manage-data.types';
import {
  TRANSACTION_HISTORY_RANGE_OPTIONS,
  TransactionAmountChartOptions,
  TransactionHistoryRange,
  TransactionHistoryRangeOption,
  buildTransactionPoints,
  createTransactionChartOptions,
  createTrendlineSeries,
  filterTransactionsByRange,
  formatCurrencyAmount,
  inferTransactionCurrencyCode,
} from '../transaction-history-dialog.utils';
import { TransactionEditorComponent } from '../transactions/transaction-editor.component';
import { TransactionOption } from '../transactions/transactions.types';

@Component({
  selector: 'jhi-payees',
  templateUrl: './payees.component.html',
  styleUrls: ['./payees.component.scss'],
  imports: [SharedModule, ReactiveFormsModule, DialogModule, CheckboxModule, NgApexchartsModule, TransactionEditorComponent],
})
export class PayeesComponent implements OnInit {
  protected payees: ManagedPayee[] = [];
  protected dialogPayees: ManagedPayee[] = [];
  protected accounts: FinancialAccount[] = [];
  protected transferAccountOptions: TransactionOption[] = [];
  protected searchText = '';
  protected includeHidden = false;
  protected isLoading = false;
  protected transactionsLoading = false;
  protected variantsLoading = false;
  protected isSaving = false;
  protected errorMessage: string | null = null;
  protected transactionsErrorMessage: string | null = null;
  protected editorVisible = false;
  protected variantEditorVisible = false;
  protected deleteCandidate: ManagedPayee | null = null;
  protected deleteCandidateMode: 'payee' | 'variant' = 'payee';
  protected editingPayee: ManagedPayee | null = null;
  protected editingVariant: ManagedPayee | null = null;
  protected selectedTransactionPayee: ManagedPayee | null = null;
  protected payeeTransactions: FinancialTransaction[] = [];
  protected payeeTransactionsFilter = '';
  protected payeeTransactionsRange: TransactionHistoryRange = 'all';
  protected payeeTrendlineVisible = false;
  protected selectedPayeeTransaction: FinancialTransaction | null = null;
  protected transactionEditorVisible = false;
  protected readonly payeeTransactionRangeOptions: TransactionHistoryRangeOption[] = TRANSACTION_HISTORY_RANGE_OPTIONS;
  protected readonly transactionAmountChartOptions: TransactionAmountChartOptions = createTransactionChartOptions(value =>
    this.formatTransactionAmount(value),
  );

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    hidden: new FormControl(false, { nonNullable: true }),
  });

  protected readonly variantForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    hidden: new FormControl(false, { nonNullable: true }),
  });

  private readonly manageDataService = inject(FinanceManageDataService);
  private readonly accountListService = inject(AccountList);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  constructor(
    @Inject(LOCALE_ID) private readonly locale: string,
    @Inject(DEFAULT_CURRENCY_CODE) private readonly defaultCurrencyCode: string,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadAccounts();
  }

  get filteredPayees(): ManagedPayee[] {
    const query = this.searchText.trim().toLowerCase();
    return this.payees.filter(payee => payee.parentId == null).filter(payee => !query || this.matchesPayeeSearch(payee, query));
  }

  get dialogVariants(): ManagedPayee[] {
    if (!this.editingPayee) {
      return [];
    }

    return this.getChildren(this.editingPayee.id, this.dialogPayees);
  }

  get deleteDialogVisible(): boolean {
    return this.deleteCandidate !== null;
  }

  set deleteDialogVisible(visible: boolean) {
    if (!visible) {
      this.deleteCandidate = null;
      this.deleteCandidateMode = 'payee';
    }
  }

  get payeeTransactionsDialogVisible(): boolean {
    return this.selectedTransactionPayee !== null;
  }

  set payeeTransactionsDialogVisible(visible: boolean) {
    if (!visible) {
      this.selectedTransactionPayee = null;
      this.payeeTransactions = [];
      this.payeeTransactionsFilter = '';
      this.payeeTransactionsRange = 'all';
      this.payeeTrendlineVisible = false;
      this.transactionsErrorMessage = null;
      this.closeTransactionEditor();
    }
  }

  get filteredPayeeTransactions(): FinancialTransaction[] {
    const durationFilteredTransactions = this.filterTransactionsByRange(this.payeeTransactions, this.payeeTransactionsRange);
    const query = this.payeeTransactionsFilter.trim().toLowerCase();
    if (!query) {
      return durationFilteredTransactions;
    }

    return durationFilteredTransactions.filter(transaction =>
      [
        transaction.date,
        transaction.payeeName,
        transaction.memo,
        transaction.displayCategory,
        transaction.categoryName,
        transaction.parentCategoryName,
        transaction.whoName,
        this.getAccountName(transaction.accountId),
        String(transaction.amount),
      ]
        .filter(Boolean)
        .some(value => String(value).toLowerCase().includes(query)),
    );
  }

  get payeeTransactionsSum(): number {
    return this.filteredPayeeTransactions.reduce((sum, transaction) => sum + Number(transaction.amount ?? 0), 0);
  }

  get payeeTransactionsAverage(): number {
    if (this.filteredPayeeTransactions.length === 0) {
      return 0;
    }
    return this.payeeTransactionsSum / this.filteredPayeeTransactions.length;
  }

  get payeeTransactionSeries(): ApexAxisChartSeries {
    const transactionSeries = buildTransactionPoints(this.filteredPayeeTransactions);

    const series: ApexAxisChartSeries = [
      {
        name: 'Transaction amount',
        data: transactionSeries,
      },
    ];

    if (this.payeeTrendlineVisible) {
      const trendlineSeries = createTrendlineSeries(transactionSeries);
      if (trendlineSeries) {
        series.push(trendlineSeries.series);
      }
    }

    return series;
  }

  get payeeTransactionsCurrencyCode(): string {
    return inferTransactionCurrencyCode(
      this.filteredPayeeTransactions,
      accountId => this.getAccountCurrency(accountId),
      this.defaultCurrencyCode,
    );
  }

  get deleteDialogTitle(): string {
    return this.deleteCandidateMode === 'variant' ? 'Hide variant?' : 'Hide payee?';
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.manageDataService.getPayees(this.includeHidden).subscribe({
      next: payees => {
        this.payees = payees;
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Loading payees failed.');
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  loadAccounts(): void {
    this.accountListService.getSimple().subscribe({
      next: accounts => {
        this.accounts = accounts;
        this.transferAccountOptions = accounts.map(account => ({ id: account.id, name: account.name }));
        this.changeDetectorRef.markForCheck();
      },
      error: () => {
        this.accounts = [];
        this.transferAccountOptions = [];
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  openAddDialog(): void {
    this.editingPayee = null;
    this.errorMessage = null;
    this.dialogPayees = [];
    this.form.reset({ name: '', hidden: false });
    this.editorVisible = true;
  }

  openEditDialog(payee: ManagedPayee): void {
    this.editingPayee = payee;
    this.errorMessage = null;
    this.form.reset({
      name: payee.name,
      hidden: Boolean(payee.hidden),
    });
    this.editorVisible = true;
    this.loadDialogPayees(payee.id);
  }

  closeEditor(): void {
    this.editorVisible = false;
    this.editingPayee = null;
    this.dialogPayees = [];
    this.variantsLoading = false;
    this.closeVariantEditor();
  }

  openAddVariantDialog(): void {
    if (!this.editingPayee) {
      return;
    }

    this.editingVariant = null;
    this.variantForm.reset({ name: '', hidden: false });
    this.variantEditorVisible = true;
  }

  openEditVariantDialog(variant: ManagedPayee): void {
    this.editingVariant = variant;
    this.variantForm.reset({
      name: variant.name,
      hidden: Boolean(variant.hidden),
    });
    this.variantEditorVisible = true;
  }

  closeVariantEditor(): void {
    this.variantEditorVisible = false;
    this.editingVariant = null;
    this.variantForm.reset({ name: '', hidden: false });
  }

  openPayeeTransactions(payee: ManagedPayee): void {
    this.selectedTransactionPayee = payee;
    this.payeeTransactions = [];
    this.payeeTransactionsFilter = '';
    this.payeeTransactionsRange = 'all';
    this.payeeTrendlineVisible = false;
    this.transactionsErrorMessage = null;
    this.transactionsLoading = true;
    this.manageDataService.getPayeeTransactions(payee.id).subscribe({
      next: transactions => {
        this.payeeTransactions = transactions.map(transaction => this.normalizeTransaction(transaction));
        this.transactionsLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.transactionsErrorMessage = this.getErrorMessage(error, 'Loading payee transactions failed.');
        this.transactionsLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  openTransactionEditor(transaction: FinancialTransaction): void {
    this.selectedPayeeTransaction = transaction;
    this.transactionEditorVisible = true;
  }

  closeTransactionEditor(): void {
    this.transactionEditorVisible = false;
    this.selectedPayeeTransaction = null;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;
    const update: ManagedPayeeUpdate = {
      name: this.form.controls.name.value.trim(),
      parentId: null,
      hidden: this.form.controls.hidden.value,
    };
    const request = this.editingPayee
      ? this.manageDataService.updatePayee(this.editingPayee.id, update)
      : this.manageDataService.createPayee(update);
    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.closeEditor();
        this.changeDetectorRef.markForCheck();
        this.load();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Saving the payee failed.');
        this.isSaving = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  saveVariant(): void {
    if (!this.editingPayee) {
      return;
    }

    if (this.variantForm.invalid) {
      this.variantForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;
    const update: ManagedPayeeUpdate = {
      name: this.variantForm.controls.name.value.trim(),
      parentId: this.editingPayee.id,
      hidden: this.variantForm.controls.hidden.value,
    };
    const request = this.editingVariant
      ? this.manageDataService.updatePayee(this.editingVariant.id, update)
      : this.manageDataService.createPayee(update);
    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.closeVariantEditor();
        this.refreshPayeesForEditor(this.editingPayee?.id ?? null);
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Saving the variant failed.');
        this.isSaving = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  confirmDelete(payee: ManagedPayee, mode: 'payee' | 'variant' = 'payee'): void {
    this.deleteCandidate = payee;
    this.deleteCandidateMode = mode;
    this.errorMessage = null;
  }

  deletePayee(): void {
    if (!this.deleteCandidate) {
      return;
    }

    const deletedPayeeId = this.deleteCandidate.id;
    const parentId = this.deleteCandidateMode === 'variant' ? (this.editingPayee?.id ?? null) : null;
    this.isSaving = true;
    this.manageDataService.deletePayee(this.deleteCandidate.id).subscribe({
      next: () => {
        this.isSaving = false;
        this.deleteCandidate = null;
        this.deleteCandidateMode = 'payee';
        if (deletedPayeeId === this.selectedTransactionPayee?.id) {
          this.payeeTransactionsDialogVisible = false;
        }
        if (parentId) {
          this.refreshPayeesForEditor(parentId);
        } else {
          this.changeDetectorRef.markForCheck();
          this.load();
        }
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Deleting the payee failed.');
        this.isSaving = false;
        this.deleteCandidate = null;
        this.deleteCandidateMode = 'payee';
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  getVariantSummary(payee: ManagedPayee): string {
    if (payee.childCount === 0) {
      return 'No variants';
    }

    if (payee.childNames.length === 0) {
      return `${payee.childCount} variant${payee.childCount === 1 ? '' : 's'}`;
    }

    const remaining = payee.childCount - payee.childNames.length;
    const suffix = remaining > 0 ? ` +${remaining} more` : '';
    return `${payee.childNames.join(', ')}${suffix}`;
  }

  getVariantCountLabel(payee: ManagedPayee): string {
    if (payee.childCount === 0) {
      return 'No variants';
    }

    return `${payee.childCount} variant${payee.childCount === 1 ? '' : 's'}`;
  }

  getAccountName(accountId: string | null | undefined): string {
    return this.accounts.find(account => account.id === accountId)?.name ?? '';
  }

  getAccountCurrency(accountId: string | null | undefined): string {
    return this.accounts.find(account => account.id === accountId)?.currencyCode ?? 'AUD';
  }

  getTransactionAmountClass(transaction: FinancialTransaction): string {
    return transaction.amount >= 0 ? 'manage-amount manage-amount--positive' : 'manage-amount manage-amount--negative';
  }

  setPayeeTransactionsRange(range: TransactionHistoryRange): void {
    this.payeeTransactionsRange = range;
  }

  isPayeeTransactionsRangeSelected(range: TransactionHistoryRange): boolean {
    return this.payeeTransactionsRange === range;
  }

  togglePayeeTrendline(): void {
    this.payeeTrendlineVisible = !this.payeeTrendlineVisible;
  }

  formatTransactionAmount(value: number | null | undefined): string {
    return formatCurrencyAmount(Number(value ?? 0), this.locale, this.payeeTransactionsCurrencyCode);
  }

  noopEditorAction(): void {
    // Dialog drill-down is read-only; editing continues to live in the account register.
  }

  private loadDialogPayees(payeeId: string): void {
    this.variantsLoading = true;
    const request = this.includeHidden ? of(this.payees) : this.manageDataService.getPayees(true);
    request.subscribe({
      next: payees => {
        this.dialogPayees = payees;
        this.editingPayee = payees.find(candidate => candidate.id === payeeId) ?? this.editingPayee;
        this.variantsLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Loading payee variants failed.');
        this.dialogPayees = this.payees;
        this.variantsLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  private refreshPayeesForEditor(payeeId: string | null): void {
    const dialogRequest = payeeId
      ? this.includeHidden
        ? of<ManagedPayee[]>([])
        : this.manageDataService.getPayees(true)
      : of<ManagedPayee[]>([]);
    forkJoin({
      pagePayees: this.manageDataService.getPayees(this.includeHidden),
      dialogPayees: dialogRequest,
    }).subscribe({
      next: ({ pagePayees, dialogPayees }) => {
        this.payees = pagePayees;
        this.dialogPayees = payeeId ? (this.includeHidden ? pagePayees : dialogPayees) : [];
        if (payeeId) {
          this.editingPayee =
            this.dialogPayees.find(candidate => candidate.id === payeeId) ??
            this.payees.find(candidate => candidate.id === payeeId) ??
            null;
          if (!this.editingPayee) {
            this.closeEditor();
          }
        }
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Refreshing payees failed.');
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  private getChildren(parentId: string, source: ManagedPayee[]): ManagedPayee[] {
    return source
      .filter(payee => payee.parentId === parentId)
      .sort((left, right) => left.name.localeCompare(right.name, undefined, { sensitivity: 'base' }));
  }

  private matchesPayeeSearch(payee: ManagedPayee, query: string): boolean {
    if (payee.name.toLowerCase().includes(query)) {
      return true;
    }

    return this.getChildren(payee.id, this.payees).some(child => child.name.toLowerCase().includes(query));
  }

  private getErrorMessage(error: unknown, fallback: string): string {
    if (typeof error !== 'object' || error === null) {
      return fallback;
    }

    const errorRecord = error as { error?: { detail?: string; message?: string }; message?: string };
    return errorRecord.error?.detail ?? errorRecord.error?.message ?? errorRecord.message ?? fallback;
  }

  private normalizeTransaction(transaction: FinancialTransaction): FinancialTransaction {
    const amount = transaction.amount;
    return {
      ...transaction,
      amount,
      payment: amount < 0 ? Math.abs(amount) : 0,
      deposit: amount > 0 ? amount : 0,
      displayCategory: this.getDisplayCategory(transaction),
      tags: transaction.tags ?? [],
      tagsDisplay: transaction.tagsDisplay ?? (transaction.tags ?? []).join(', '),
    };
  }

  private getDisplayCategory(transaction: FinancialTransaction): string {
    if (transaction.splitParent) {
      return 'Split / Multiple Categories';
    }

    if (!transaction.categoryName) {
      return '';
    }

    return transaction.parentCategoryName ? `${transaction.parentCategoryName}: ${transaction.categoryName}` : transaction.categoryName;
  }

  private filterTransactionsByRange(transactions: FinancialTransaction[], range: TransactionHistoryRange): FinancialTransaction[] {
    return filterTransactionsByRange(transactions, range);
  }
}

import { ChangeDetectorRef, Component, DEFAULT_CURRENCY_CODE, Inject, LOCALE_ID, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ApexAxisChartSeries, NgApexchartsModule } from 'ng-apexcharts';

import SharedModule from 'app/shared/shared.module';
import { FinancialAccount, FinancialTransaction } from '../finance.model';
import { AccountList } from '../account-list/account-list.service';
import { FinanceManageDataService } from '../manage-data/finance-manage-data.service';
import { ManagedCategory, ManagedCategoryUpdate } from '../manage-data/finance-manage-data.types';
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

type CategoryFilter = 'all' | 'category' | 'who';

@Component({
  selector: 'jhi-categories',
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.scss'],
  imports: [SharedModule, ReactiveFormsModule, DialogModule, NgApexchartsModule, TransactionEditorComponent],
})
export class CategoriesComponent implements OnInit {
  protected categories: ManagedCategory[] = [];
  protected accounts: FinancialAccount[] = [];
  protected transferAccountOptions: TransactionOption[] = [];
  protected searchText = '';
  protected classificationFilter: CategoryFilter = 'category';
  protected isLoading = false;
  protected transactionsLoading = false;
  protected isSaving = false;
  protected errorMessage: string | null = null;
  protected transactionsErrorMessage: string | null = null;
  protected editorVisible = false;
  protected deleteCandidate: ManagedCategory | null = null;
  protected editingCategory: ManagedCategory | null = null;
  protected selectedTransactionCategory: ManagedCategory | null = null;
  protected categoryTransactions: FinancialTransaction[] = [];
  protected categoryTransactionsFilter = '';
  protected categoryTransactionsRange: TransactionHistoryRange = 'all';
  protected categoryTrendlineVisible = false;
  protected selectedCategoryTransaction: FinancialTransaction | null = null;
  protected transactionEditorVisible = false;
  protected expandedCategoryIds = new Set<string>();
  protected readonly categoryTransactionRangeOptions: TransactionHistoryRangeOption[] = TRANSACTION_HISTORY_RANGE_OPTIONS;
  protected readonly transactionAmountChartOptions: TransactionAmountChartOptions = createTransactionChartOptions(value =>
    this.formatTransactionAmount(value),
  );

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    classificationId: new FormControl(0, { nonNullable: true }),
    parentId: new FormControl<string | null>(null),
    comment: new FormControl<string | null>(null),
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

  get visibleTreeCategories(): ManagedCategory[] {
    const rows: ManagedCategory[] = [];
    const roots = this.getChildren(null);
    roots.forEach(category => this.appendVisibleCategory(rows, category));
    return rows;
  }

  get parentOptions(): ManagedCategory[] {
    const classificationId = this.form.controls.classificationId.value;
    return this.categories.filter(category => category.id !== this.editingCategory?.id && category.classificationId === classificationId);
  }

  get deleteDialogVisible(): boolean {
    return this.deleteCandidate !== null;
  }

  set deleteDialogVisible(visible: boolean) {
    if (!visible) {
      this.deleteCandidate = null;
    }
  }

  get categoryTransactionsDialogVisible(): boolean {
    return this.selectedTransactionCategory !== null;
  }

  set categoryTransactionsDialogVisible(visible: boolean) {
    if (!visible) {
      this.selectedTransactionCategory = null;
      this.categoryTransactions = [];
      this.categoryTransactionsFilter = '';
      this.categoryTransactionsRange = 'all';
      this.categoryTrendlineVisible = false;
      this.transactionsErrorMessage = null;
      this.closeTransactionEditor();
    }
  }

  get filteredCategoryTransactions(): FinancialTransaction[] {
    const durationFilteredTransactions = this.filterTransactionsByRange(this.categoryTransactions, this.categoryTransactionsRange);
    const query = this.categoryTransactionsFilter.trim().toLowerCase();
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
        String(transaction.amount ?? ''),
      ]
        .filter(Boolean)
        .some(value => String(value).toLowerCase().includes(query)),
    );
  }

  get categoryTransactionsSum(): number {
    return this.filteredCategoryTransactions.reduce((sum, transaction) => sum + Number(transaction.amount ?? 0), 0);
  }

  get categoryTransactionsAverage(): number {
    if (this.filteredCategoryTransactions.length === 0) {
      return 0;
    }
    return this.categoryTransactionsSum / this.filteredCategoryTransactions.length;
  }

  get categoryTransactionSeries(): ApexAxisChartSeries {
    const transactionSeries = buildTransactionPoints(this.filteredCategoryTransactions);

    const series: ApexAxisChartSeries = [
      {
        name: 'Transaction amount',
        data: transactionSeries,
      },
    ];

    if (this.categoryTrendlineVisible) {
      const trendlineSeries = createTrendlineSeries(transactionSeries);
      if (trendlineSeries) {
        series.push(trendlineSeries.series);
      }
    }

    return series;
  }

  get categoryTransactionsCurrencyCode(): string {
    return inferTransactionCurrencyCode(
      this.filteredCategoryTransactions,
      accountId => this.getAccountCurrency(accountId),
      this.defaultCurrencyCode,
    );
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.manageDataService.getCategories().subscribe({
      next: categories => {
        this.categories = categories;
        this.expandedCategoryIds = new Set([
          ...this.expandedCategoryIds,
          ...categories.filter(category => category.parentId == null).map(category => category.id),
        ]);
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Loading categories failed.');
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
    this.editingCategory = null;
    this.errorMessage = null;
    this.form.reset({ name: '', classificationId: this.classificationFilter === 'who' ? 1 : 0, parentId: null, comment: null });
    this.editorVisible = true;
  }

  openEditDialog(category: ManagedCategory): void {
    this.editingCategory = category;
    this.errorMessage = null;
    this.form.reset({
      name: category.name,
      classificationId: category.classificationId,
      parentId: category.parentId,
      comment: category.comment,
    });
    this.editorVisible = true;
  }

  openCategoryTransactions(category: ManagedCategory): void {
    this.selectedTransactionCategory = category;
    this.categoryTransactions = [];
    this.categoryTransactionsFilter = '';
    this.categoryTransactionsRange = 'all';
    this.categoryTrendlineVisible = false;
    this.transactionsErrorMessage = null;
    this.transactionsLoading = true;
    this.manageDataService.getCategoryTransactions(category.id).subscribe({
      next: transactions => {
        this.categoryTransactions = transactions.map(transaction => this.normalizeTransaction(transaction));
        this.transactionsLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.transactionsErrorMessage = this.getErrorMessage(error, 'Loading category transactions failed.');
        this.transactionsLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  openTransactionEditor(transaction: FinancialTransaction): void {
    this.selectedCategoryTransaction = transaction;
    this.transactionEditorVisible = true;
  }

  closeTransactionEditor(): void {
    this.transactionEditorVisible = false;
    this.selectedCategoryTransaction = null;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;
    const update: ManagedCategoryUpdate = {
      name: this.form.controls.name.value.trim(),
      classificationId: this.form.controls.classificationId.value,
      parentId: this.form.controls.parentId.value || null,
      comment: this.form.controls.comment.value?.trim() || null,
    };
    const request = this.editingCategory
      ? this.manageDataService.updateCategory(this.editingCategory.id, update)
      : this.manageDataService.createCategory(update);
    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.editorVisible = false;
        this.changeDetectorRef.markForCheck();
        this.load();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Saving the category failed.');
        this.isSaving = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  confirmDelete(category: ManagedCategory): void {
    this.deleteCandidate = category;
    this.errorMessage = null;
  }

  deleteCategory(): void {
    if (!this.deleteCandidate) {
      return;
    }

    this.isSaving = true;
    this.manageDataService.deleteCategory(this.deleteCandidate.id).subscribe({
      next: () => {
        this.isSaving = false;
        this.deleteCandidate = null;
        this.changeDetectorRef.markForCheck();
        this.load();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Deleting the category failed.');
        this.isSaving = false;
        this.deleteCandidate = null;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  toggleCategory(category: ManagedCategory): void {
    const nextExpandedCategoryIds = new Set(this.expandedCategoryIds);
    if (nextExpandedCategoryIds.has(category.id)) {
      nextExpandedCategoryIds.delete(category.id);
    } else {
      nextExpandedCategoryIds.add(category.id);
    }
    this.expandedCategoryIds = nextExpandedCategoryIds;
  }

  isCategoryExpanded(category: ManagedCategory): boolean {
    return this.hasSearchQuery() || this.expandedCategoryIds.has(category.id);
  }

  hasVisibleChildren(category: ManagedCategory): boolean {
    return this.getChildren(category.id).some(child => this.shouldShowCategoryBranch(child));
  }

  getCategoryDepth(category: ManagedCategory): number {
    let depth = 0;
    let parentId = category.parentId;
    while (parentId) {
      const parent = this.categories.find(candidate => candidate.id === parentId);
      if (!parent) {
        break;
      }
      depth += 1;
      parentId = parent.parentId;
    }
    return depth;
  }

  getClassificationLabel(classificationId: number): string {
    if (classificationId === 1) {
      return 'Who';
    }
    return 'Category';
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

  setCategoryTransactionsRange(range: TransactionHistoryRange): void {
    this.categoryTransactionsRange = range;
  }

  isCategoryTransactionsRangeSelected(range: TransactionHistoryRange): boolean {
    return this.categoryTransactionsRange === range;
  }

  toggleCategoryTrendline(): void {
    this.categoryTrendlineVisible = !this.categoryTrendlineVisible;
  }

  formatTransactionAmount(value: number | null | undefined): string {
    return formatCurrencyAmount(Number(value ?? 0), this.locale, this.categoryTransactionsCurrencyCode);
  }

  noopEditorAction(): void {
    // Dialog drill-down is read-only; editing continues to live in the account register.
  }

  private getErrorMessage(error: any, fallback: string): string {
    return error?.error?.detail ?? error?.error?.message ?? error?.message ?? fallback;
  }

  private appendVisibleCategory(rows: ManagedCategory[], category: ManagedCategory): void {
    if (!this.shouldShowCategoryBranch(category)) {
      return;
    }

    rows.push(category);
    if (!this.isCategoryExpanded(category)) {
      return;
    }

    this.getChildren(category.id).forEach(child => this.appendVisibleCategory(rows, child));
  }

  private shouldShowCategoryBranch(category: ManagedCategory): boolean {
    if (!this.matchesTypeFilter(category)) {
      return false;
    }

    if (!this.hasSearchQuery()) {
      return true;
    }

    return this.matchesSearch(category) || this.getChildren(category.id).some(child => this.shouldShowCategoryBranch(child));
  }

  private getChildren(parentId: string | null): ManagedCategory[] {
    return this.categories
      .filter(category => (category.parentId ?? null) === parentId)
      .filter(category => this.matchesTypeFilter(category))
      .sort((left, right) => left.name.localeCompare(right.name));
  }

  private matchesTypeFilter(category: ManagedCategory): boolean {
    return (
      this.classificationFilter === 'all' ||
      (this.classificationFilter === 'category' && category.classificationId === 0) ||
      (this.classificationFilter === 'who' && category.classificationId === 1)
    );
  }

  private matchesSearch(category: ManagedCategory): boolean {
    const query = this.searchText.trim().toLowerCase();
    return (
      !query ||
      category.name.toLowerCase().includes(query) ||
      category.displayName.toLowerCase().includes(query) ||
      (category.parentName ?? '').toLowerCase().includes(query) ||
      (category.comment ?? '').toLowerCase().includes(query)
    );
  }

  private hasSearchQuery(): boolean {
    return this.searchText.trim().length > 0;
  }

  private normalizeTransaction(transaction: FinancialTransaction): FinancialTransaction {
    const amount = Number(transaction.amount ?? 0);
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

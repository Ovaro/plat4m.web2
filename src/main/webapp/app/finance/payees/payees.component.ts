import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';

import SharedModule from 'app/shared/shared.module';
import { FinancialAccount, FinancialTransaction } from '../finance.model';
import { AccountList } from '../account-list/account-list.service';
import { FinanceManageDataService } from '../manage-data/finance-manage-data.service';
import { ManagedPayee, ManagedPayeeUpdate } from '../manage-data/finance-manage-data.types';
import { TransactionEditorComponent } from '../transactions/transaction-editor.component';
import { TransactionOption } from '../transactions/transactions.types';

@Component({
  selector: 'jhi-payees',
  templateUrl: './payees.component.html',
  styleUrls: ['./payees.component.scss'],
  imports: [SharedModule, ReactiveFormsModule, DialogModule, TransactionEditorComponent],
})
export class PayeesComponent implements OnInit {
  protected payees: ManagedPayee[] = [];
  protected accounts: FinancialAccount[] = [];
  protected transferAccountOptions: TransactionOption[] = [];
  protected searchText = '';
  protected includeHidden = false;
  protected isLoading = false;
  protected transactionsLoading = false;
  protected isSaving = false;
  protected errorMessage: string | null = null;
  protected transactionsErrorMessage: string | null = null;
  protected editorVisible = false;
  protected deleteCandidate: ManagedPayee | null = null;
  protected editingPayee: ManagedPayee | null = null;
  protected selectedTransactionPayee: ManagedPayee | null = null;
  protected payeeTransactions: FinancialTransaction[] = [];
  protected payeeTransactionsFilter = '';
  protected selectedPayeeTransaction: FinancialTransaction | null = null;
  protected transactionEditorVisible = false;

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    parentId: new FormControl<string | null>(null),
    hidden: new FormControl(false, { nonNullable: true }),
  });

  private readonly manageDataService = inject(FinanceManageDataService);
  private readonly accountListService = inject(AccountList);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.load();
    this.loadAccounts();
  }

  get filteredPayees(): ManagedPayee[] {
    const query = this.searchText.trim().toLowerCase();
    return this.payees.filter(payee => {
      const parentName = this.getParentName(payee.parentId);
      return !query || payee.name.toLowerCase().includes(query) || parentName.toLowerCase().includes(query);
    });
  }

  get parentOptions(): ManagedPayee[] {
    return this.payees.filter(payee => payee.id !== this.editingPayee?.id && !payee.hidden);
  }

  get deleteDialogVisible(): boolean {
    return this.deleteCandidate !== null;
  }

  set deleteDialogVisible(visible: boolean) {
    if (!visible) {
      this.deleteCandidate = null;
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
      this.transactionsErrorMessage = null;
    }
  }

  get filteredPayeeTransactions(): FinancialTransaction[] {
    const query = this.payeeTransactionsFilter.trim().toLowerCase();
    if (!query) {
      return this.payeeTransactions;
    }

    return this.payeeTransactions.filter(transaction =>
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
    this.form.reset({ name: '', parentId: null, hidden: false });
    this.editorVisible = true;
  }

  openEditDialog(payee: ManagedPayee): void {
    this.editingPayee = payee;
    this.errorMessage = null;
    this.form.reset({
      name: payee.name,
      parentId: payee.parentId,
      hidden: Boolean(payee.hidden),
    });
    this.editorVisible = true;
  }

  openPayeeTransactions(payee: ManagedPayee): void {
    this.selectedTransactionPayee = payee;
    this.payeeTransactions = [];
    this.payeeTransactionsFilter = '';
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
      parentId: this.form.controls.parentId.value ?? null,
      hidden: this.form.controls.hidden.value,
    };
    const request = this.editingPayee
      ? this.manageDataService.updatePayee(this.editingPayee.id, update)
      : this.manageDataService.createPayee(update);
    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.editorVisible = false;
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

  confirmDelete(payee: ManagedPayee): void {
    this.deleteCandidate = payee;
    this.errorMessage = null;
  }

  deletePayee(): void {
    if (!this.deleteCandidate) {
      return;
    }

    this.isSaving = true;
    this.manageDataService.deletePayee(this.deleteCandidate.id).subscribe({
      next: () => {
        this.isSaving = false;
        this.deleteCandidate = null;
        this.changeDetectorRef.markForCheck();
        this.load();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Deleting the payee failed.');
        this.isSaving = false;
        this.deleteCandidate = null;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  getParentName(parentId: string | null): string {
    if (!parentId) {
      return 'None';
    }

    return this.payees.find(payee => payee.id === parentId)?.name ?? 'Unknown';
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

  noopEditorAction(): void {
    // Dialog drill-down is read-only; editing continues to live in the account register.
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
}

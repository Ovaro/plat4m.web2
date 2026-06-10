import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectButtonModule } from 'primeng/selectbutton';
import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { FinancialTransaction } from '../finance.model';
import { Transactions } from './transactions.service';
import { TransactionEditorMode, TransactionLookupQuery, TransactionOption, TransactionUpdate } from './transactions.types';

type TransactionEditorType = 'deposit' | 'withdrawal' | 'transfer';

@Component({
  selector: 'jhi-transaction-editor',
  imports: [CommonModule, ReactiveFormsModule, AutoCompleteModule, SelectButtonModule, InputNumberModule],
  templateUrl: './transaction-editor.component.html',
  styleUrls: ['./transaction-editor.component.scss'],
})
export class TransactionEditorComponent implements OnInit, OnChanges {
  private static readonly LOOKUP_PAGE_SIZE = 50;

  @Input() transaction: FinancialTransaction | null = null;
  @Input() mode: TransactionEditorMode = 'view';
  @Input() readonlyReason: string | null = null;
  @Input() currencyCode = 'AUD';
  @Input() currentAccountId: string | null = null;
  @Input() accounts: TransactionOption[] = [];
  @Input() saving = false;

  @Output() saveTransaction = new EventEmitter<TransactionUpdate>();
  @Output() newTransaction = new EventEmitter<void>();
  @Output() editTransaction = new EventEmitter<void>();
  @Output() cancelEditor = new EventEmitter<void>();

  protected categorySuggestions: TransactionOption[] = [];
  protected whoSuggestions: TransactionOption[] = [];
  protected payeeSuggestions: TransactionOption[] = [];
  protected transferAccountSuggestions: TransactionOption[] = [];
  protected tagSuggestions: string[] = [];
  protected categoryLoading = false;
  protected whoLoading = false;
  protected payeeLoading = false;

  protected readonly form = new FormGroup({
    transactionType: new FormControl<TransactionEditorType>('withdrawal', { nonNullable: true }),
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl<number | null>(0, { validators: [Validators.required, Validators.min(0)] }),
    payee: new FormControl<TransactionOption | null>(null),
    transferAccount: new FormControl<TransactionOption | null>(null),
    tags: new FormControl<string[]>([], { nonNullable: true }),
    category: new FormControl<TransactionOption | null>(null),
    who: new FormControl<TransactionOption | null>(null),
    memo: new FormControl<string | null>(null),
    cleared: new FormControl(false, { nonNullable: true }),
  });
  protected readonly tagInput = new FormControl('', { nonNullable: true });

  protected readonly transactionTypeOptions = [
    { label: 'Deposit', value: 'deposit' },
    { label: 'Withdrawal', value: 'withdrawal' },
    { label: 'Transfer', value: 'transfer' },
  ];

  private readonly transactionsService = inject(Transactions);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const selectedPayee = this.getSelectedPayeeOption();
    const selectedCategory = this.getSelectedCategoryOption();
    const selectedWho = this.getSelectedWhoOption();
    const selectedTransferAccount = this.getSelectedTransferAccountOption();

    this.payeeSuggestions = selectedPayee ? [selectedPayee] : [];
    this.categorySuggestions = selectedCategory ? [selectedCategory] : [];
    this.whoSuggestions = selectedWho ? [selectedWho] : [];
    this.transferAccountSuggestions = selectedTransferAccount ? [selectedTransferAccount] : [];
    this.form.controls.transactionType.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.applyFormState();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ('transaction' in changes) {
      this.patchForm();
    }

    this.applyFormState();
    this.ensureSelectedSuggestionsPresent();
  }

  onSubmit(): void {
    const effectiveReadonlyReason = this.getEffectiveReadonlyReason();
    if (this.form.invalid || effectiveReadonlyReason) {
      return;
    }

    const rawValue = this.form.getRawValue();
    const normalizedAmount = Math.abs(rawValue.amount ?? 0);
    const signedAmount = this.getSignedAmount(rawValue.transactionType, normalizedAmount);
    const isTransfer = rawValue.transactionType === 'transfer';

    this.saveTransaction.emit({
      date: rawValue.date,
      amount: signedAmount,
      payeeId: isTransfer ? null : (rawValue.payee?.id ?? null),
      payeeName: isTransfer ? null : this.trimToNull(rawValue.payee?.name ?? null),
      categoryId: isTransfer ? null : (rawValue.category?.id ?? null),
      whoId: isTransfer ? null : (rawValue.who?.id ?? null),
      tags: rawValue.tags,
      transferredAccountId: isTransfer ? (rawValue.transferAccount?.id ?? null) : null,
      memo: this.trimToNull(rawValue.memo),
      cleared: rawValue.cleared,
    });
  }

  getEffectiveReadonlyReason(): string | null {
    return this.mode === 'view' ? null : this.readonlyReason;
  }

  getReadonlyBannerReason(): string | null {
    return this.readonlyReason;
  }

  isViewMode(): boolean {
    return this.mode === 'view';
  }

  isAddMode(): boolean {
    return this.mode === 'add';
  }

  canEdit(): boolean {
    return this.mode === 'view' && !this.readonlyReason;
  }

  getTransferSummary(): string {
    if (this.isAddMode()) {
      return 'Outgoing transfer from this account';
    }

    return this.transaction && this.transaction.amount >= 0 ? 'Incoming transfer' : 'Outgoing transfer';
  }

  searchPayees(event: AutoCompleteCompleteEvent): void {
    this.loadPayees(event.query);
  }

  searchCategories(event: AutoCompleteCompleteEvent): void {
    this.loadCategories(event.query);
  }

  searchWho(event: AutoCompleteCompleteEvent): void {
    this.loadWho(event.query);
  }

  clearPayee(): void {
    this.form.controls.payee.setValue(null);
  }

  clearCategory(): void {
    this.form.controls.category.setValue(null);
  }

  clearWho(): void {
    this.form.controls.who.setValue(null);
  }

  onTagInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',' || event.key === 'Tab') {
      event.preventDefault();
      this.commitTagInput();
      return;
    }

    if (event.key === 'Backspace' && !this.tagInput.value && this.form.controls.tags.value.length > 0) {
      const tags = [...this.form.controls.tags.value];
      tags.pop();
      this.form.controls.tags.setValue(tags);
      this.form.controls.tags.markAsDirty();
    }
  }

  onTagInputBlur(): void {
    this.commitTagInput();
  }

  removeTag(tagToRemove: string): void {
    const updatedTags = this.form.controls.tags.value.filter(tag => tag !== tagToRemove);
    this.form.controls.tags.setValue(updatedTags);
    this.form.controls.tags.markAsDirty();
  }

  searchTags(query: string): void {
    const trimmedQuery = query.trim();
    const req: TransactionLookupQuery = { page: 0, size: TransactionEditorComponent.LOOKUP_PAGE_SIZE, query: trimmedQuery || undefined };
    this.transactionsService.getTagOptions(req).subscribe({
      next: response => {
        const existingTags = new Set(this.form.controls.tags.value.map(tag => tag.toLowerCase()));
        this.tagSuggestions = (response.body ?? []).map(option => option.name).filter(tag => !existingTags.has(tag.toLowerCase()));
      },
      error: () => {
        this.tagSuggestions = [];
      },
    });
  }

  searchTransferAccounts(event: AutoCompleteCompleteEvent): void {
    const normalizedQuery = event.query.trim().toLowerCase();
    const filteredOptions = this.getAvailableTransferAccounts().filter(option => option.name.toLowerCase().includes(normalizedQuery));
    this.transferAccountSuggestions = this.mergeSelectedSuggestion(filteredOptions, this.getSelectedTransferAccountControlOption());
  }

  clearTransferAccount(): void {
    this.form.controls.transferAccount.setValue(null);
  }

  isTransferMode(): boolean {
    return this.form.controls.transactionType.value === 'transfer';
  }

  private patchForm(): void {
    const transactionType = this.getTransactionType();

    this.form.reset({
      transactionType,
      date: this.transaction?.date ?? '',
      amount: Math.abs(this.transaction?.amount ?? 0),
      payee: this.getSelectedPayeeOption(),
      transferAccount: this.getSelectedTransferAccountOption(),
      tags: this.transaction?.tags ?? [],
      category: this.getSelectedCategoryOption(),
      who: this.getSelectedWhoOption(),
      memo: this.transaction?.memo ?? null,
      cleared: this.transaction?.cleared ?? false,
    });
    this.tagInput.setValue('');
  }

  private trimToNull(value: string | null): string | null {
    if (value == null) {
      return null;
    }

    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private getTransactionType(): TransactionEditorType {
    if (this.transaction?.transferredAccountId) {
      return 'transfer';
    }

    return (this.transaction?.amount ?? 0) >= 0 ? 'deposit' : 'withdrawal';
  }

  private applyFormState(): void {
    this.form.enable({ emitEvent: false });

    const transactionType = this.form.controls.transactionType.value;
    if (transactionType === 'transfer') {
      this.form.controls.transferAccount.setValidators([Validators.required]);
      this.form.controls.transferAccount.updateValueAndValidity({ emitEvent: false });
      this.form.controls.payee.disable({ emitEvent: false });
      this.form.controls.category.disable({ emitEvent: false });
      this.form.controls.who.disable({ emitEvent: false });
      this.form.controls.transferAccount.enable({ emitEvent: false });
    } else {
      this.form.controls.transferAccount.clearValidators();
      this.form.controls.transferAccount.updateValueAndValidity({ emitEvent: false });
      this.form.controls.transferAccount.disable({ emitEvent: false });
      this.form.controls.who.enable({ emitEvent: false });
    }

    if (this.mode === 'view' || this.readonlyReason) {
      this.form.disable({ emitEvent: false });
      return;
    }

    this.form.controls.transactionType.enable({ emitEvent: false });
  }

  private loadPayees(query: string): void {
    this.payeeLoading = true;
    const req: TransactionLookupQuery = { page: 0, size: TransactionEditorComponent.LOOKUP_PAGE_SIZE, query };
    this.transactionsService.getPayeeOptions(req).subscribe({
      next: response => {
        this.payeeLoading = false;
        this.payeeSuggestions = this.mergeSelectedSuggestion(response.body ?? [], this.getSelectedPayeeOption());
      },
      error: () => {
        this.payeeLoading = false;
      },
    });
  }

  private loadCategories(query: string): void {
    this.categoryLoading = true;
    const req: TransactionLookupQuery = { page: 0, size: TransactionEditorComponent.LOOKUP_PAGE_SIZE, query };
    this.transactionsService.getCategoryOptions(req).subscribe({
      next: response => {
        this.categoryLoading = false;
        this.categorySuggestions = this.mergeSelectedSuggestion(response.body ?? [], this.getSelectedCategoryOption());
      },
      error: () => {
        this.categoryLoading = false;
      },
    });
  }

  private loadWho(query: string): void {
    this.whoLoading = true;
    const req: TransactionLookupQuery = { page: 0, size: TransactionEditorComponent.LOOKUP_PAGE_SIZE, query };
    this.transactionsService.getWhoOptions(req).subscribe({
      next: response => {
        this.whoLoading = false;
        this.whoSuggestions = this.mergeSelectedSuggestion(response.body ?? [], this.getSelectedWhoControlOption());
      },
      error: () => {
        this.whoLoading = false;
      },
    });
  }

  private ensureSelectedSuggestionsPresent(): void {
    this.payeeSuggestions = this.mergeSelectedSuggestion(this.payeeSuggestions, this.getSelectedPayeeOption());
    this.categorySuggestions = this.mergeSelectedSuggestion(this.categorySuggestions, this.getSelectedCategoryOption());
    this.whoSuggestions = this.mergeSelectedSuggestion(this.whoSuggestions, this.getSelectedWhoControlOption());
    this.transferAccountSuggestions = this.mergeSelectedSuggestion(
      this.transferAccountSuggestions,
      this.getSelectedTransferAccountControlOption(),
    );
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

  private getSelectedPayeeOption(): TransactionOption | null {
    const transaction = this.transaction;
    if (!transaction?.payeeId || !transaction.payeeName) {
      return null;
    }

    return {
      id: transaction.payeeId,
      name: transaction.payeeName,
    };
  }

  private getSelectedCategoryOption(): TransactionOption | null {
    const transaction = this.transaction;
    if (!transaction?.categoryId || !transaction.displayCategory) {
      return null;
    }

    return {
      id: transaction.categoryId,
      name: transaction.displayCategory,
    };
  }

  private getSelectedWhoOption(): TransactionOption | null {
    const transaction = this.transaction;
    if (!transaction?.whoId || !transaction.whoName) {
      return null;
    }

    return {
      id: transaction.whoId,
      name: transaction.whoName,
    };
  }

  private getSelectedTransferAccountOption(): TransactionOption | null {
    const transaction = this.transaction;
    if (!transaction?.transferredAccountId) {
      return null;
    }

    return this.accounts.find(account => account.id === transaction.transferredAccountId) ?? null;
  }

  private getSelectedTransferAccountControlOption(): TransactionOption | null {
    return this.form.controls.transferAccount.value;
  }

  private getSelectedWhoControlOption(): TransactionOption | null {
    return this.form.controls.who.value ?? this.getSelectedWhoOption();
  }

  private getAvailableTransferAccounts(): TransactionOption[] {
    return this.accounts.filter(account => account.id !== this.currentAccountId);
  }

  private getSignedAmount(transactionType: TransactionEditorType, normalizedAmount: number): number {
    if (transactionType === 'deposit') {
      return normalizedAmount;
    }

    if (transactionType === 'transfer') {
      if (this.transaction?.transferredAccountId) {
        return this.transaction.amount >= 0 ? normalizedAmount : normalizedAmount * -1;
      }

      return normalizedAmount * -1;
    }

    return normalizedAmount * -1;
  }

  private commitTagInput(): void {
    const inputValue = this.tagInput.value;
    if (!inputValue.trim()) {
      this.tagInput.setValue('');
      return;
    }

    const existingKeys = new Set(this.form.controls.tags.value.map(tag => tag.toLowerCase()));
    const nextTags = [...this.form.controls.tags.value];

    for (const part of inputValue.split(',')) {
      const trimmed = part.trim();
      if (!trimmed) {
        continue;
      }

      const normalized = trimmed.toLowerCase();
      if (existingKeys.has(normalized)) {
        continue;
      }

      existingKeys.add(normalized);
      nextTags.push(trimmed);
    }

    this.form.controls.tags.setValue(nextTags);
    this.form.controls.tags.markAsDirty();
    this.tagInput.setValue('');
    this.tagSuggestions = [];
  }
}

import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { OverlayOnShowEvent } from 'primeng/api';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TreeSelectModule } from 'primeng/treeselect';
import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { FinancialTransaction } from '../finance.model';
import { Transactions } from './transactions.service';
import {
  TransactionEditorDraftRequest,
  TransactionEditorMode,
  TransactionEditorSelectableType,
  TransactionCategoryType,
  TransactionLookupQuery,
  TransactionOption,
  TransactionSplitUpdate,
  TransactionTreeOption,
  TransactionUpdate,
} from './transactions.types';

type TransactionEditorType = TransactionEditorSelectableType | 'split';

interface TransactionSplitEditorRow {
  category: TransactionTreeOption | null;
  who: TransactionTreeOption | null;
  memo: string | null;
  amount: number | null;
}

interface PendingCategoryCreation {
  rawName: string;
  parentName: string;
  childName: string | null;
  type: TransactionCategoryType;
  parentCategoryId: string | null;
  targetSplitRowIndex: number | null;
  saving: boolean;
  error: string | null;
}

interface PendingTransactionTypeChange {
  targetType: TransactionEditorSelectableType;
  title: string;
  message: string;
  confirmLabel?: string;
  alternateLabel?: string;
}

@Component({
  selector: 'jhi-transaction-editor',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    AutoCompleteModule,
    SelectButtonModule,
    InputNumberModule,
    TreeSelectModule,
    FontAwesomeModule,
  ],
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
  @Input() dialogView = false;
  @Input() initialTransactionType: TransactionEditorSelectableType | null = null;

  @Output() saveTransaction = new EventEmitter<TransactionUpdate>();
  @Output() newTransaction = new EventEmitter<TransactionEditorDraftRequest | void>();
  @Output() editTransaction = new EventEmitter<void>();
  @Output() deleteTransaction = new EventEmitter<void>();
  @Output() cancelEditor = new EventEmitter<void>();

  protected categoryOptions: TransactionTreeOption[] = [];
  protected whoOptions: TransactionTreeOption[] = [];
  protected payeeSuggestions: TransactionOption[] = [];
  protected transferAccountSuggestions: TransactionOption[] = [];
  protected tagSuggestions: string[] = [];
  protected categoryLoading = false;
  protected whoLoading = false;
  protected payeeLoading = false;
  protected splitRows: TransactionSplitEditorRow[] = [];
  protected splitDraftRows: TransactionSplitEditorRow[] = [];
  protected isSplitEditorOpen = false;
  protected splitRowsLoading = false;
  protected splitDraftConvertsTransaction = false;
  protected pendingCategoryCreation: PendingCategoryCreation | null = null;
  protected pendingTransactionTypeChange: PendingTransactionTypeChange | null = null;
  protected actionsMenuOpen = false;
  protected deleteConfirmOpen = false;

  protected readonly form = new FormGroup({
    transactionType: new FormControl<TransactionEditorType>('withdrawal', { nonNullable: true }),
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl<number | null>(0, { validators: [Validators.required, Validators.min(0)] }),
    payee: new FormControl<TransactionOption | string | null>(null),
    transferAccount: new FormControl<TransactionOption | null>(null),
    tags: new FormControl<string[]>([], { nonNullable: true }),
    category: new FormControl<TransactionTreeOption | null>(null),
    who: new FormControl<TransactionTreeOption | null>(null),
    memo: new FormControl<string | null>(null),
    cleared: new FormControl(false, { nonNullable: true }),
  });
  protected readonly tagInput = new FormControl('', { nonNullable: true });

  protected readonly transactionTypeOptions = [
    { label: 'Withdrawal', value: 'withdrawal' },
    { label: 'Deposit', value: 'deposit' },
    { label: 'Transfer', value: 'transfer' },
  ];

  private readonly transactionsService = inject(Transactions);
  private readonly destroyRef = inject(DestroyRef);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private splitRowsLoadSequence = 0;
  private suppressTransactionTypePrompt = false;
  private lastSelectableTransactionType: TransactionEditorType = 'withdrawal';
  private replaceWithTransferOnSave = false;

  ngOnInit(): void {
    const selectedPayee = this.getSelectedPayeeOption();
    const selectedTransferAccount = this.getSelectedTransferAccountOption();

    this.payeeSuggestions = selectedPayee ? [selectedPayee] : [];
    this.transferAccountSuggestions = selectedTransferAccount ? [selectedTransferAccount] : [];
    this.loadCategoryTreeOptions();
    this.loadWhoTreeOptions();
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
    const splitPayload = this.isSplitMode() ? this.buildSplitPayload(this.splitRows) : null;
    const reconciledAmount = splitPayload?.length ? this.getSplitRowsTotal(splitPayload) : normalizedAmount;
    const signedAmount = this.getSignedAmount(rawValue.transactionType, reconciledAmount);
    const isTransfer = rawValue.transactionType === 'transfer';
    const isSplit = rawValue.transactionType === 'split';
    const payeeValue = this.normalizePayeeValue(rawValue.payee);
    const whoId = this.normalizeWhoValue(rawValue.who);
    this.saveTransaction.emit({
      date: rawValue.date,
      amount: signedAmount,
      payeeId: isTransfer ? null : payeeValue.id,
      payeeName: isTransfer ? null : payeeValue.name,
      categoryId: isTransfer || isSplit ? null : this.normalizeCategoryValue(rawValue.category),
      whoId: isTransfer || isSplit ? null : whoId,
      tags: rawValue.tags,
      transferredAccountId: isTransfer ? (rawValue.transferAccount?.id ?? null) : null,
      memo: this.trimToNull(rawValue.memo),
      cleared: rawValue.cleared,
      splits: splitPayload,
      replaceWithTransfer: isTransfer && this.replaceWithTransferOnSave,
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

  getPayeeLabel(): string {
    if (this.form.controls.transactionType.value === 'deposit') {
      return 'To';
    }

    return 'Pay to';
  }

  onTransactionTypeSelectionChange(nextType: TransactionEditorSelectableType): void {
    if (this.suppressTransactionTypePrompt) {
      this.lastSelectableTransactionType = nextType;
      return;
    }

    const previousType = this.lastSelectableTransactionType;
    if (nextType === previousType) {
      return;
    }

    if (!this.shouldInterceptTransactionTypeChange(previousType, nextType)) {
      this.lastSelectableTransactionType = nextType;
      if (nextType !== 'transfer') {
        this.replaceWithTransferOnSave = false;
      }
      return;
    }

    this.pendingTransactionTypeChange = this.buildPendingTransactionTypeChange(previousType, nextType);
    this.setTransactionTypeSilently(previousType);
  }

  searchPayees(event: AutoCompleteCompleteEvent): void {
    this.loadPayees(event.query);
  }

  clearPayee(): void {
    this.form.controls.payee.setValue(null);
  }

  clearCategory(): void {
    this.form.controls.category.setValue(null);
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

  openNewCategoryDialog(): void {
    this.openCategoryCreateDialog('', null, this.form.controls.category.value);
  }

  openNewSplitCategoryDialog(index: number): void {
    this.openCategoryCreateDialog('', index, this.splitDraftRows[index]?.category ?? null);
  }

  cancelCategoryCreateDialog(): void {
    if (this.pendingCategoryCreation?.saving) {
      return;
    }

    this.pendingCategoryCreation = null;
  }

  onPendingCategoryNameChange(): void {
    const pendingCategory = this.pendingCategoryCreation;
    if (!pendingCategory) {
      return;
    }

    if (pendingCategory.parentCategoryId) {
      pendingCategory.childName = this.trimToNull(pendingCategory.rawName);
      pendingCategory.error = null;
      return;
    }

    const parsedCategory = this.parseCategoryName(pendingCategory.rawName);
    pendingCategory.parentName = parsedCategory?.parentName ?? '';
    pendingCategory.childName = parsedCategory?.childName ?? null;
    pendingCategory.error = null;
    const inferredType = this.inferCategoryTypeFromParent(pendingCategory.parentName);
    if (inferredType) {
      pendingCategory.type = inferredType;
    }
  }

  confirmCategoryCreateDialog(): void {
    const pendingCategory = this.pendingCategoryCreation;
    if (!pendingCategory || pendingCategory.saving) {
      return;
    }

    const parsedCategory = pendingCategory.parentCategoryId
      ? this.parseChildCategoryName(pendingCategory.rawName, pendingCategory.parentName)
      : this.parseCategoryName(pendingCategory.rawName);
    if (!parsedCategory) {
      pendingCategory.error = pendingCategory.parentCategoryId ? 'Enter a sub-category name.' : 'Enter a category name.';
      return;
    }

    pendingCategory.parentName = parsedCategory.parentName;
    pendingCategory.childName = parsedCategory.childName;
    pendingCategory.rawName = pendingCategory.parentCategoryId ? parsedCategory.childName! : parsedCategory.rawName;
    pendingCategory.saving = true;
    pendingCategory.error = null;
    this.transactionsService
      .createCategory({
        name: pendingCategory.parentCategoryId ? parsedCategory.childName! : pendingCategory.rawName,
        type: pendingCategory.type,
        parentCategoryId: pendingCategory.parentCategoryId,
      })
      .subscribe({
        next: category => {
          const categoryNode = { key: category.id, label: category.name, leaf: true };
          if (pendingCategory.targetSplitRowIndex == null) {
            this.form.controls.category.setValue(categoryNode);
            this.form.controls.category.markAsDirty();
          } else if (this.splitDraftRows[pendingCategory.targetSplitRowIndex]) {
            this.splitDraftRows[pendingCategory.targetSplitRowIndex].category = categoryNode;
          }
          this.form.markAsDirty();
          this.pendingCategoryCreation = null;
          this.loadCategoryTreeOptions();
        },
        error: error => {
          pendingCategory.saving = false;
          pendingCategory.error = error?.error?.detail ?? error?.message ?? 'Creating the category failed.';
        },
      });
  }

  getCategoryCreationTitle(): string {
    return this.pendingCategoryCreation?.parentCategoryId || this.pendingCategoryCreation?.childName
      ? 'Add sub-category?'
      : 'Add category?';
  }

  getCategoryCreationMessage(): string {
    const pendingCategory = this.pendingCategoryCreation;
    if (!pendingCategory) {
      return '';
    }

    if (pendingCategory.parentCategoryId) {
      const childName = this.trimToNull(pendingCategory.rawName);
      return childName
        ? `Create "${childName}" as a sub-category of "${pendingCategory.parentName}"?`
        : `Create a new sub-category under "${pendingCategory.parentName}".`;
    }

    if (pendingCategory.childName) {
      return `Create "${pendingCategory.childName}" as a sub-category of "${pendingCategory.parentName}"?`;
    }

    return `Create "${pendingCategory.parentName}" as a new ${pendingCategory.type} category?`;
  }

  shouldShowCategoryTypeChoice(): boolean {
    const pendingCategory = this.pendingCategoryCreation;
    if (!pendingCategory?.parentName) {
      return true;
    }

    if (pendingCategory.parentCategoryId) {
      return false;
    }

    return this.inferCategoryTypeFromParent(pendingCategory.parentName) == null;
  }

  clearWho(): void {
    this.form.controls.who.setValue(null);
  }

  toggleActionsMenu(): void {
    this.actionsMenuOpen = !this.actionsMenuOpen;
  }

  openDeleteConfirmation(): void {
    this.actionsMenuOpen = false;
    this.deleteConfirmOpen = true;
  }

  cancelDeleteConfirmation(): void {
    this.deleteConfirmOpen = false;
  }

  confirmDeleteTransaction(): void {
    this.deleteConfirmOpen = false;
    this.deleteTransaction.emit();
  }

  onTagInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.commitTagInput();
      return;
    }

    if (event.key === 'Tab') {
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

  onFormKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter') {
      return;
    }

    const target = event.target;
    if (!(target instanceof HTMLElement)) {
      return;
    }

    if (target instanceof HTMLTextAreaElement || target instanceof HTMLButtonElement) {
      return;
    }

    if (target.classList.contains('transaction-editor__tag-input')) {
      return;
    }

    event.preventDefault();
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

  isSplitMode(): boolean {
    return this.form.controls.transactionType.value === 'split';
  }

  openSplitEditor(): void {
    if (this.splitRowsLoading) {
      return;
    }

    this.splitDraftConvertsTransaction = false;
    this.splitDraftRows = this.cloneSplitRows(this.splitRows.length ? this.splitRows : [this.createEmptySplitRow()]);
    this.isSplitEditorOpen = true;
  }

  enableSplitMode(): void {
    if (this.isViewMode() || this.getEffectiveReadonlyReason()) {
      return;
    }

    this.splitDraftConvertsTransaction = true;
    this.splitDraftRows = [this.createSplitRowFromCurrentTransaction()];
    this.isSplitEditorOpen = true;
  }

  cancelSplitEditor(): void {
    this.isSplitEditorOpen = false;
    this.splitDraftRows = [];
    this.splitDraftConvertsTransaction = false;
  }

  doneSplitEditor(): void {
    if (this.isViewMode() || this.getEffectiveReadonlyReason()) {
      this.cancelSplitEditor();
      return;
    }

    const nextSplitRows = this.cloneSplitRows(this.splitDraftRows).filter(
      row => row.category || row.who || this.trimToNull(row.memo) || row.amount != null,
    );
    const total = this.getSplitDraftTotal();
    if (nextSplitRows.length > 0) {
      this.splitRows = nextSplitRows;
      if (this.splitDraftConvertsTransaction) {
        this.form.controls.transactionType.setValue('split');
        this.form.controls.category.setValue(null);
        this.form.controls.who.setValue(null);
      }
      this.form.controls.amount.setValue(total);
      this.form.controls.amount.markAsDirty();
      this.form.markAsDirty();
    } else {
      this.splitRows = [];
      if (this.isSplitMode()) {
        this.form.controls.transactionType.setValue(this.getNonSplitTransactionType());
        this.form.markAsDirty();
      }
    }

    this.cancelSplitEditor();
  }

  addSplitRow(): void {
    if (this.isViewMode() || this.getEffectiveReadonlyReason()) {
      return;
    }

    this.splitDraftRows = [...this.splitDraftRows, this.createEmptySplitRow()];
  }

  deleteSplitRow(index: number): void {
    if (this.isViewMode() || this.getEffectiveReadonlyReason()) {
      return;
    }

    this.splitDraftRows = this.splitDraftRows.filter((_row, rowIndex) => rowIndex !== index);
  }

  getCommittedSplitTotal(): number {
    return this.getSplitRowsTotal(this.buildSplitPayload(this.splitRows));
  }

  getSplitDraftTotal(): number {
    return this.getSplitRowsTotal(this.buildSplitPayload(this.splitDraftRows));
  }

  getSourceTransactionTotal(): number {
    return Math.abs(this.form.controls.amount.value ?? 0);
  }

  isSplitDraftTotalDifferent(): boolean {
    return Math.abs(this.getSplitDraftTotal() - this.getSourceTransactionTotal()) >= 0.005;
  }

  getSplitButtonLabel(): string {
    if (this.splitRowsLoading) {
      return 'Loading splits...';
    }

    const count = this.splitRows.length;
    if (count === 0) {
      return 'Itemise split';
    }

    return `${count} split ${count === 1 ? 'row' : 'rows'} · ${this.formatMoney(this.getCommittedSplitTotal())}`;
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-AU', { style: 'currency', currency: this.currencyCode }).format(value);
  }

  confirmTransactionTypeChange(): void {
    const pendingChange = this.pendingTransactionTypeChange;
    if (!pendingChange) {
      return;
    }

    this.pendingTransactionTypeChange = null;
    if (pendingChange.targetType === 'transfer') {
      this.replaceWithTransferOnSave = true;
    }
    this.setTransactionTypeSilently(pendingChange.targetType);
  }

  chooseNewTransferTransaction(): void {
    const pendingChange = this.pendingTransactionTypeChange;
    this.pendingTransactionTypeChange = null;
    this.replaceWithTransferOnSave = false;
    this.setTransactionTypeSilently(this.lastSelectableTransactionType);
    if (!pendingChange) {
      return;
    }

    this.newTransaction.emit({
      initialTransactionType: pendingChange.targetType,
      date: this.form.controls.date.value,
      amount: Math.abs(this.form.controls.amount.value ?? Math.abs(this.transaction?.amount ?? 0)),
      memo: this.trimToNull(this.form.controls.memo.value),
      cleared: this.form.controls.cleared.value,
      tags: [...this.form.controls.tags.value],
    });
  }

  cancelTransactionTypeChange(): void {
    this.pendingTransactionTypeChange = null;
    this.replaceWithTransferOnSave = false;
    this.setTransactionTypeSilently(this.lastSelectableTransactionType);
  }

  private patchForm(): void {
    const transactionType = this.isAddMode() && this.initialTransactionType ? this.initialTransactionType : this.getTransactionType();

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
    this.isSplitEditorOpen = false;
    this.splitDraftRows = [];
    this.splitRows = [];
    this.pendingTransactionTypeChange = null;
    this.actionsMenuOpen = false;
    this.deleteConfirmOpen = false;
    this.replaceWithTransferOnSave = false;
    this.lastSelectableTransactionType = transactionType;
    this.loadSplitRows();
  }

  private trimToNull(value: string | null): string | null {
    if (value == null) {
      return null;
    }

    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private getTransactionType(): TransactionEditorType {
    if (this.transaction?.splitParent) {
      return 'split';
    }

    if (this.transaction?.transferredAccountId) {
      return 'transfer';
    }

    return (this.transaction?.amount ?? 0) >= 0 ? 'deposit' : 'withdrawal';
  }

  private getNonSplitTransactionType(): TransactionEditorType {
    if (this.transaction?.transferredAccountId) {
      return 'transfer';
    }

    const amount = this.form.controls.amount.value ?? Math.abs(this.transaction?.amount ?? 0);
    if (
      (this.transaction?.amount ?? 0) > 0 ||
      (!this.transaction && amount > 0 && this.form.controls.transactionType.value === 'deposit')
    ) {
      return 'deposit';
    }

    return this.form.controls.transactionType.value === 'deposit' ? 'deposit' : 'withdrawal';
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
    } else if (transactionType === 'split') {
      this.form.controls.transferAccount.clearValidators();
      this.form.controls.transferAccount.updateValueAndValidity({ emitEvent: false });
      this.form.controls.transferAccount.disable({ emitEvent: false });
      this.form.controls.category.disable({ emitEvent: false });
      this.form.controls.who.disable({ emitEvent: false });
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

  private shouldInterceptTransactionTypeChange(previousType: TransactionEditorType, nextType: TransactionEditorSelectableType): boolean {
    if (this.mode !== 'edit' || this.readonlyReason || !this.transaction?.id) {
      return false;
    }

    if ((previousType === 'deposit' || previousType === 'withdrawal') && (nextType === 'deposit' || nextType === 'withdrawal')) {
      return true;
    }

    return previousType !== 'transfer' && nextType === 'transfer';
  }

  private buildPendingTransactionTypeChange(
    previousType: TransactionEditorType,
    nextType: TransactionEditorSelectableType,
  ): PendingTransactionTypeChange {
    if ((previousType === 'deposit' || previousType === 'withdrawal') && (nextType === 'deposit' || nextType === 'withdrawal')) {
      return {
        targetType: nextType,
        title: `Change to ${this.getTransactionTypeLabel(nextType)}?`,
        message: `This will convert the existing transaction from ${this.getTransactionTypeLabel(previousType).toLowerCase()} to ${this.getTransactionTypeLabel(nextType).toLowerCase()} when you save it.`,
        confirmLabel: `Yes, change it`,
      };
    }

    return {
      targetType: nextType,
      title: 'Change to transfer?',
      message: 'You can either create a new transfer transaction, or convert this existing transaction into a transfer when you save.',
      confirmLabel: 'Convert existing',
      alternateLabel: 'Create new transfer',
    };
  }

  private getTransactionTypeLabel(type: TransactionEditorType): string {
    switch (type) {
      case 'deposit':
        return 'Deposit';
      case 'transfer':
        return 'Transfer';
      case 'split':
        return 'Split';
      default:
        return 'Withdrawal';
    }
  }

  private setTransactionTypeSilently(transactionType: TransactionEditorType): void {
    this.suppressTransactionTypePrompt = true;
    this.form.controls.transactionType.setValue(transactionType);
    this.lastSelectableTransactionType = transactionType;
    this.suppressTransactionTypePrompt = false;
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

  private loadCategoryTreeOptions(): void {
    this.categoryLoading = true;
    this.transactionsService.getCategoryTreeOptions().subscribe({
      next: options => {
        this.categoryLoading = false;
        this.categoryOptions = options;
        const selectedCategory = this.form.controls.category.value;
        if (selectedCategory?.key) {
          this.form.controls.category.setValue(this.findCategoryOptionByKey(selectedCategory.key) ?? selectedCategory, {
            emitEvent: false,
          });
        }
      },
      error: () => {
        this.categoryLoading = false;
        this.categoryOptions = [];
      },
    });
  }

  private ensureSelectedSuggestionsPresent(): void {
    this.payeeSuggestions = this.mergeSelectedSuggestion(this.payeeSuggestions, this.getSelectedPayeeOption());
    this.transferAccountSuggestions = this.mergeSelectedSuggestion(
      this.transferAccountSuggestions,
      this.getSelectedTransferAccountControlOption(),
    );
  }

  private loadWhoTreeOptions(): void {
    this.whoLoading = true;
    this.transactionsService.getWhoTreeOptions().subscribe({
      next: options => {
        this.whoLoading = false;
        this.whoOptions = options;
        const selectedWho = this.form.controls.who.value;
        if (selectedWho?.key) {
          this.form.controls.who.setValue(this.findWhoOptionByKey(selectedWho.key) ?? selectedWho, { emitEvent: false });
        }
      },
      error: () => {
        this.whoLoading = false;
        this.whoOptions = [];
      },
    });
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

  private openCategoryCreateDialog(
    rawName: string,
    targetSplitRowIndex: number | null,
    selectedParentCategory: TransactionTreeOption | null = null,
  ): void {
    const normalizedParentCategory = this.getCategoryCreateParent(selectedParentCategory);
    const parentCategoryId = this.trimToNull(normalizedParentCategory?.key ?? null);
    const parentName = parentCategoryId ? (this.getCategoryPathByKey(parentCategoryId) ?? normalizedParentCategory?.label ?? '') : '';
    const parsedCategory = parentCategoryId ? null : this.parseCategoryName(rawName);
    const inferredType = parentCategoryId
      ? this.inferCategoryTypeFromKey(parentCategoryId)
      : this.inferCategoryTypeFromParent(parsedCategory?.parentName ?? '');
    this.pendingCategoryCreation = {
      rawName: parentCategoryId ? '' : (parsedCategory?.rawName ?? ''),
      parentName: parentCategoryId ? parentName : (parsedCategory?.parentName ?? ''),
      childName: parentCategoryId ? null : (parsedCategory?.childName ?? null),
      type: inferredType ?? (this.form.controls.transactionType.value === 'deposit' ? 'income' : 'expense'),
      parentCategoryId,
      targetSplitRowIndex,
      saving: false,
      error: null,
    };
  }

  private parseCategoryName(category: string): { rawName: string; parentName: string; childName: string | null } | null {
    const normalizedCategory = this.trimToNull(category);
    if (!normalizedCategory) {
      return null;
    }

    const parts = normalizedCategory.split(':', 2).map(part => part.trim());
    if (!parts[0] || (parts.length > 1 && !parts[1])) {
      return null;
    }

    return {
      rawName: parts.length > 1 ? `${parts[0]}: ${parts[1]}` : parts[0],
      parentName: parts[0],
      childName: parts.length > 1 ? parts[1] : null,
    };
  }

  private parseChildCategoryName(
    childName: string,
    parentName: string,
  ): { rawName: string; parentName: string; childName: string | null } | null {
    const normalizedChildName = this.trimToNull(childName);
    const normalizedParentName = this.trimToNull(parentName);
    if (!normalizedChildName || !normalizedParentName) {
      return null;
    }

    return {
      rawName: `${normalizedParentName}: ${normalizedChildName}`,
      parentName: normalizedParentName,
      childName: normalizedChildName,
    };
  }

  private inferCategoryTypeFromParent(parentName: string): TransactionCategoryType | null {
    const parentNode = this.findCategoryOptionByLabel(parentName);
    if (!parentNode?.key) {
      return null;
    }

    const rootNode = this.findCategoryRootForKey(parentNode.key);
    if (rootNode?.label?.toLowerCase() === 'income') {
      return 'income';
    }
    if (rootNode?.label?.toLowerCase() === 'expense') {
      return 'expense';
    }

    return null;
  }

  private inferCategoryTypeFromKey(key: string): TransactionCategoryType | null {
    const rootNode = this.findCategoryRootForKey(key);
    if (rootNode?.label?.toLowerCase() === 'income') {
      return 'income';
    }
    if (rootNode?.label?.toLowerCase() === 'expense') {
      return 'expense';
    }

    return null;
  }

  private normalizePayeeValue(payee: TransactionOption | string | null): { id: string | null; name: string | null } {
    if (payee == null) {
      return { id: null, name: null };
    }

    if (typeof payee === 'string') {
      return { id: null, name: this.trimToNull(payee) };
    }

    return {
      id: payee.id,
      name: this.trimToNull(payee.name),
    };
  }

  private normalizeWhoValue(who: TransactionTreeOption | null): string | null {
    if (who == null) {
      return null;
    }

    return this.trimToNull(who.key);
  }

  private normalizeCategoryValue(category: TransactionTreeOption | null): string | null {
    if (category == null) {
      return null;
    }

    return this.trimToNull(category.key);
  }

  private getSelectedWhoOption(): TransactionTreeOption | null {
    const transaction = this.transaction;
    if (!transaction?.whoId || !transaction.whoName) {
      return null;
    }

    return this.findWhoOptionByKey(transaction.whoId) ?? { key: transaction.whoId, label: transaction.whoName, leaf: true };
  }

  private getSelectedCategoryOption(): TransactionTreeOption | null {
    const transaction = this.transaction;
    if (!transaction?.categoryId || !transaction.displayCategory) {
      return null;
    }

    return (
      this.findCategoryOptionByKey(transaction.categoryId) ?? {
        key: transaction.categoryId,
        label: transaction.displayCategory,
        leaf: true,
      }
    );
  }

  private findCategoryOptionByKey(key: string): TransactionTreeOption | null {
    return this.findTreeOptionByKeyInNodes(this.categoryOptions, key);
  }

  private findCategoryOptionByLabel(label: string): TransactionTreeOption | null {
    const normalizedLabel = this.trimToNull(label)?.toLowerCase();
    if (!normalizedLabel) {
      return null;
    }

    return this.findTreeOptionByLabelInNodes(this.categoryOptions, normalizedLabel);
  }

  private findCategoryRootForKey(key: string): TransactionTreeOption | null {
    for (const node of this.categoryOptions) {
      if (node.key === key || this.findTreeOptionByKeyInNodes(node.children ?? [], key)) {
        return node;
      }
    }

    return null;
  }

  private getCategoryPathByKey(key: string): string | null {
    const path = this.findTreeOptionPathByKey(this.categoryOptions, key);
    if (!path.length) {
      return null;
    }

    const categoryPath = path.filter(label => !['income', 'expense'].includes(label.toLowerCase()));
    return categoryPath.length ? categoryPath.join(' : ') : path[path.length - 1];
  }

  private getCategoryCreateParent(selectedCategory: TransactionTreeOption | null): TransactionTreeOption | null {
    if (!selectedCategory?.key) {
      return null;
    }

    const path = this.findTreeOptionNodePathByKey(this.categoryOptions, selectedCategory.key);
    if (path.length <= 1) {
      return null;
    }

    // Only allow Income/Expense > Parent > Child. If a child is selected,
    // create under its parent rather than creating a grandchild.
    return path[1];
  }

  private findWhoOptionByKey(key: string): TransactionTreeOption | null {
    return this.findTreeOptionByKeyInNodes(this.whoOptions, key);
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

  private findTreeOptionPathByKey(nodes: TransactionTreeOption[], key: string, parentPath: string[] = []): string[] {
    for (const node of nodes) {
      const nextPath = [...parentPath, node.label];
      if (node.key === key) {
        return nextPath;
      }

      const childPath = this.findTreeOptionPathByKey(node.children ?? [], key, nextPath);
      if (childPath.length) {
        return childPath;
      }
    }

    return [];
  }

  private findTreeOptionNodePathByKey(
    nodes: TransactionTreeOption[],
    key: string,
    parentPath: TransactionTreeOption[] = [],
  ): TransactionTreeOption[] {
    for (const node of nodes) {
      const nextPath = [...parentPath, node];
      if (node.key === key) {
        return nextPath;
      }

      const childPath = this.findTreeOptionNodePathByKey(node.children ?? [], key, nextPath);
      if (childPath.length) {
        return childPath;
      }
    }

    return [];
  }

  private findTreeOptionByLabelInNodes(nodes: TransactionTreeOption[], normalizedLabel: string): TransactionTreeOption | null {
    for (const node of nodes) {
      if (node.label.toLowerCase() === normalizedLabel) {
        return node;
      }

      const childMatch = this.findTreeOptionByLabelInNodes(node.children ?? [], normalizedLabel);
      if (childMatch) {
        return childMatch;
      }
    }

    return null;
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

  private getAvailableTransferAccounts(): TransactionOption[] {
    return this.accounts.filter(account => account.id !== this.currentAccountId);
  }

  private getSignedAmount(transactionType: TransactionEditorType, normalizedAmount: number): number {
    if (transactionType === 'deposit') {
      return normalizedAmount;
    }

    if (transactionType === 'split') {
      return this.transaction && this.transaction.amount > 0 ? normalizedAmount : normalizedAmount * -1;
    }

    if (transactionType === 'transfer') {
      if (this.transaction?.transferredAccountId) {
        return this.transaction.amount >= 0 ? normalizedAmount : normalizedAmount * -1;
      }

      return normalizedAmount * -1;
    }

    return normalizedAmount * -1;
  }

  private loadSplitRows(): void {
    const loadSequence = ++this.splitRowsLoadSequence;
    if (!this.transaction?.splitParent || !this.currentAccountId || !this.transaction.id) {
      this.splitRowsLoading = false;
      return;
    }

    this.splitRowsLoading = true;
    this.transactionsService
      .getSplits(this.currentAccountId, this.transaction.id)
      .pipe(
        finalize(() => {
          if (loadSequence === this.splitRowsLoadSequence) {
            this.splitRowsLoading = false;
            this.changeDetectorRef.markForCheck();
          }
        }),
      )
      .subscribe({
        next: splits => {
          if (loadSequence !== this.splitRowsLoadSequence) {
            return;
          }

          try {
            this.splitRows = splits.map(split => this.mapSplitUpdateToEditorRow(split));
          } catch (error) {
            console.error('Failed to map transaction splits', {
              transactionId: this.transaction?.id,
              splits,
              error,
            });
            this.splitRows = [];
          }
        },
        error: error => {
          if (loadSequence !== this.splitRowsLoadSequence) {
            return;
          }

          console.error('Failed to load transaction splits', {
            accountId: this.currentAccountId,
            transactionId: this.transaction?.id,
            error,
          });
          this.splitRows = [];
        },
      });
  }

  private mapSplitUpdateToEditorRow(split: TransactionSplitUpdate): TransactionSplitEditorRow {
    return {
      category: split.categoryId
        ? (this.findCategoryOptionByKey(split.categoryId) ?? { key: split.categoryId, label: split.categoryName ?? 'Category', leaf: true })
        : null,
      who: split.whoId ? (this.findWhoOptionByKey(split.whoId) ?? { key: split.whoId, label: split.whoName ?? 'Who', leaf: true }) : null,
      memo: split.memo,
      amount: Math.abs(split.amount ?? 0),
    };
  }

  private buildSplitPayload(rows: TransactionSplitEditorRow[]): TransactionSplitUpdate[] {
    return rows
      .map(row => {
        const categoryId = this.normalizeCategoryValue(row.category);
        const whoId = this.normalizeWhoValue(row.who);
        const memo = this.trimToNull(row.memo);
        const hasAmount = row.amount != null;
        return {
          categoryId,
          whoId,
          memo,
          amount: Math.abs(row.amount ?? 0),
          hasContent: Boolean(categoryId || whoId || memo || hasAmount),
        };
      })
      .filter(row => row.hasContent)
      .map(({ hasContent: _hasContent, ...row }) => row);
  }

  private getSplitRowsTotal(rows: TransactionSplitUpdate[]): number {
    return rows.reduce((total, row) => total + Math.abs(row.amount ?? 0), 0);
  }

  private createEmptySplitRow(): TransactionSplitEditorRow {
    return {
      category: null,
      who: null,
      memo: null,
      amount: null,
    };
  }

  private createSplitRowFromCurrentTransaction(): TransactionSplitEditorRow {
    return {
      category: this.form.controls.category.value ?? this.getSelectedCategoryOption(),
      who: this.form.controls.who.value ?? this.getSelectedWhoOption(),
      memo: this.form.controls.memo.value,
      amount: Math.abs(this.form.controls.amount.value ?? this.transaction?.amount ?? 0),
    };
  }

  private cloneSplitRows(rows: TransactionSplitEditorRow[]): TransactionSplitEditorRow[] {
    return rows.map(row => ({ ...row }));
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

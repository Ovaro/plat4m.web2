export interface TransactionOption {
  id: string;
  name: string;
}

export interface TransactionTreeOption {
  key: string;
  label: string;
  selectable?: boolean;
  leaf?: boolean;
  children?: TransactionTreeOption[];
}

export interface TransactionLookupQuery {
  page: number;
  size: number;
  query?: string;
}

export type TransactionCategoryType = 'income' | 'expense';

export interface TransactionCategoryCreateRequest {
  name: string;
  type: TransactionCategoryType;
  parentCategoryId?: string | null;
}

export interface TransactionSplitUpdate {
  categoryId: string | null;
  categoryName?: string | null;
  whoId: string | null;
  whoName?: string | null;
  memo: string | null;
  amount: number;
}

export type TransactionEditorSelectableType = 'withdrawal' | 'deposit' | 'transfer';

export interface TransactionUpdate {
  date: string;
  amount: number;
  payeeId: string | null;
  payeeName: string | null;
  categoryId: string | null;
  whoId: string | null;
  tags: string[];
  transferredAccountId: string | null;
  memo: string | null;
  cleared: boolean;
  splits?: TransactionSplitUpdate[] | null;
  replaceWithTransfer?: boolean | null;
}

export interface TransactionEditorDraftRequest {
  initialTransactionType?: TransactionEditorSelectableType | null;
  date?: string;
  amount?: number | null;
  memo?: string | null;
  cleared?: boolean;
  tags?: string[];
}

export interface TransactionGridQuery {
  page: number;
  size: number;
  sort: string[];
  filters?: string;
}

export type TransactionEditorMode = 'view' | 'edit' | 'add';

export interface TransactionOption {
  id: string;
  name: string;
}

export interface TransactionLookupQuery {
  page: number;
  size: number;
  query?: string;
}

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
}

export interface TransactionGridQuery {
  page: number;
  size: number;
  sort: string[];
  filters?: string;
}

export type TransactionEditorMode = 'view' | 'edit' | 'add';

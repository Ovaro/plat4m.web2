import { TransactionOption, TransactionTreeOption } from '../transactions/transactions.types';

export interface TransactionImportDraftRequest {
  rawContent: string;
  rawHtml?: string | null;
  expectedEndingBalance?: number | null;
}

export interface TransactionImportRow {
  id: string;
  rowIndex: number;
  date: string | null;
  amount: number | null;
  transactionKind: string | null;
  payeeText: string | null;
  transferAccountText: string | null;
  memo: string | null;
  runningBalance: number | null;
  aiCategoryGuess: string | null;
  aiConfidence: number | null;
  resolvedPayeeId: string | null;
  resolvedPayeeName: string | null;
  resolvedCategoryId: string | null;
  resolvedCategoryName: string | null;
  resolvedTransferAccountId: string | null;
  resolvedTransferAccountName: string | null;
  payeeNeedsReview: boolean;
  categoryNeedsReview: boolean;
  transferNeedsReview: boolean;
  externalTransferLike: boolean;
  dateNeedsReview: boolean;
  amountNeedsReview: boolean;
  balanceMismatch: boolean;
  duplicateSuspected: boolean;
  duplicateStrongMatch: boolean;
  duplicateTransactionId: string | null;
  duplicateTransactionDate: string | null;
  duplicateTransactionAmount: number | null;
  duplicateTransactionPayeeName: string | null;
  duplicateConfirmed: boolean;
  duplicateRejected: boolean;
  accepted: boolean;
  ignored: boolean;
  reviewMessage?: string | null;
  payeeControl?: TransactionOption | string | null;
  categoryControl?: TransactionTreeOption | null;
  transferAccountControl?: TransactionOption | null;
  transferDisplayMode?: boolean;
}

export interface TransactionImportDraft {
  importId: string;
  accountId: string;
  accountName: string;
  status: string;
  correlationStatus: string;
  correlationMessage: string;
  totalRows: number;
  flaggedRows: number;
  createdDateTime: string;
  rows: TransactionImportRow[];
}

export interface TransactionImportHistoryItem {
  importId: string;
  accountId: string;
  accountName: string;
  status: string;
  createdDateTime: string;
  totalRows: number;
  flaggedRows: number;
  correlationStatus: string;
}

export interface TransactionImportCommitResponse {
  importId: string;
  createdTransactionCount: number;
  correlationStatus: string;
  correlationMessage: string;
  focusTransactionId?: string | null;
  focusRowIndex?: number | null;
}

export interface TransactionImportCommitRequest {
  autoAcceptUnhandled?: boolean;
}

export interface TransactionImportRowUpdate {
  date?: string | null;
  amount?: number | null;
  payeeText?: string | null;
  resolvedTransferAccountId?: string | null;
  externalTransferLike?: boolean | null;
  resolvedPayeeId?: string | null;
  resolvedCategoryId?: string | null;
  memo?: string | null;
  accepted?: boolean;
  ignored?: boolean;
  applyDuplicateResolution?: boolean;
  duplicateConfirmed?: boolean;
  duplicateRejected?: boolean;
}

export interface ResourceOption {
  id: string;
  name: string;
}

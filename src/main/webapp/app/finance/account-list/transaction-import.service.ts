import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, tap } from 'rxjs/operators';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  ResourceOption,
  TransactionImportCommitRequest,
  TransactionImportCommitResponse,
  TransactionImportDraft,
  TransactionImportDraftRequest,
  TransactionImportHistoryItem,
  TransactionImportRow,
  TransactionImportRowUpdate,
} from './transaction-import.types';

@Injectable({ providedIn: 'root' })
export class TransactionImportService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  createDraft(accountId: string, request: TransactionImportDraftRequest): Observable<TransactionImportDraft> {
    console.log('[txn-import] POST createDraft:start', { accountId, rawLength: request.rawContent?.length ?? 0 });
    return this.http
      .post<TransactionImportDraft>(this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transaction-imports`), request)
      .pipe(
        tap({
          next: draft =>
            console.log('[txn-import] POST createDraft:next', { accountId, importId: draft.importId, rows: draft.rows?.length ?? 0 }),
          error: error => console.error('[txn-import] POST createDraft:error', { accountId, error }),
        }),
        finalize(() => console.log('[txn-import] POST createDraft:finalize', { accountId })),
      );
  }

  getAccountImports(accountId: string): Observable<TransactionImportHistoryItem[]> {
    return this.http
      .get<TransactionImportHistoryItem[]>(this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transaction-imports`))
      .pipe(
        tap({
          next: history =>
            console.log('[txn-import] GET accountImports:next', {
              accountId,
              count: history.length,
              importIds: history.map(item => item.importId),
            }),
          error: error => console.error('[txn-import] GET accountImports:error', { accountId, error }),
        }),
      );
  }

  getImport(importId: string): Observable<TransactionImportDraft> {
    return this.http.get<TransactionImportDraft>(this.applicationConfigService.getEndpointFor(`api/transaction-imports/${importId}`)).pipe(
      tap({
        next: draft =>
          console.log('[txn-import] GET import:next', { importId, rows: draft.rows?.length ?? 0, flaggedRows: draft.flaggedRows }),
        error: error => console.error('[txn-import] GET import:error', { importId, error }),
      }),
    );
  }

  updateRow(importId: string, rowId: string, update: TransactionImportRowUpdate): Observable<TransactionImportRow> {
    console.log('[txn-import] PUT updateRow:start', { importId, rowId, update });
    return this.http
      .put<TransactionImportRow>(this.applicationConfigService.getEndpointFor(`api/transaction-imports/${importId}/rows/${rowId}`), update)
      .pipe(
        tap({
          next: row =>
            console.log('[txn-import] PUT updateRow:next', {
              importId,
              rowId,
              duplicateSuspected: row.duplicateSuspected,
              duplicateConfirmed: row.duplicateConfirmed,
              accepted: row.accepted,
              ignored: row.ignored,
            }),
          error: error => console.error('[txn-import] PUT updateRow:error', { importId, rowId, error }),
        }),
      );
  }

  commitImport(importId: string, request: TransactionImportCommitRequest = {}): Observable<TransactionImportCommitResponse> {
    return this.http.post<TransactionImportCommitResponse>(
      this.applicationConfigService.getEndpointFor(`api/transaction-imports/${importId}/commit`),
      request,
    );
  }

  backOutImport(importId: string): Observable<null> {
    return this.http.post<null>(this.applicationConfigService.getEndpointFor(`api/transaction-imports/${importId}/back-out`), {});
  }

  discardImport(importId: string): Observable<null> {
    return this.http.post<null>(this.applicationConfigService.getEndpointFor(`api/transaction-imports/${importId}/discard`), {});
  }

  searchPayees(query: string): Observable<ResourceOption[]> {
    return this.http.get<ResourceOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/payees'), {
      params: { query, size: 20 },
    });
  }

  searchCategories(query: string): Observable<ResourceOption[]> {
    return this.http.get<ResourceOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/categories'), {
      params: { query, size: 20 },
    });
  }

  getLastCategoryForPayee(payeeId: string, transactionType: 'deposit' | 'withdrawal'): Observable<ResourceOption> {
    return this.http.get<ResourceOption>(
      this.applicationConfigService.getEndpointFor(`api/transactions/editor-options/payees/${payeeId}/last-category`),
      {
        params: { type: transactionType },
      },
    );
  }
}

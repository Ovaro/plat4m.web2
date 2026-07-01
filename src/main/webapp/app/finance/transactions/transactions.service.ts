import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOption } from 'app/core/request/request-util';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinancialTransaction } from '../finance.model';
import { Pagination } from 'app/core/request/request.model';
import {
  TransactionCategoryCreateRequest,
  TransactionLookupQuery,
  TransactionOption,
  TransactionSplitUpdate,
  TransactionTreeOption,
  TransactionUpdate,
} from './transactions.types';

@Injectable({
  providedIn: 'root', // Add this line
})
export class Transactions {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  get(accountId: string, req?: Pagination): Observable<HttpResponse<FinancialTransaction[]>> {
    const options = createRequestOption(req);
    return this.http.get<FinancialTransaction[]>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions-paging`),
      {
        params: options,
        observe: 'response',
      },
    );
  }

  getById(accountId: string, transactionId: string): Observable<FinancialTransaction> {
    return this.http.get<FinancialTransaction>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions/${transactionId}`),
    );
  }

  getLinkedTransfer(accountId: string, transactionId: string): Observable<FinancialTransaction> {
    return this.http.get<FinancialTransaction>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions/${transactionId}/linked-transfer`),
    );
  }

  getCategoryOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/categories'), {
      params: options,
      observe: 'response',
    });
  }

  getCategoryTreeOptions(): Observable<TransactionTreeOption[]> {
    return this.http.get<TransactionTreeOption[]>(
      this.applicationConfigService.getEndpointFor('api/transactions/editor-options/categories-tree'),
    );
  }

  createCategory(request: TransactionCategoryCreateRequest): Observable<TransactionOption> {
    return this.http.post<TransactionOption>(
      this.applicationConfigService.getEndpointFor('api/transactions/editor-options/categories'),
      request,
    );
  }

  getPayeeOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/payees'), {
      params: options,
      observe: 'response',
    });
  }

  getLastCategoryForPayee(payeeId: string, transactionType: 'deposit' | 'withdrawal'): Observable<TransactionOption | null> {
    return this.http.get<TransactionOption | null>(
      this.applicationConfigService.getEndpointFor(`api/transactions/editor-options/payees/${payeeId}/last-category`),
      {
        params: { type: transactionType },
      },
    );
  }

  getWhoOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/who'), {
      params: options,
      observe: 'response',
    });
  }

  getWhoTreeOptions(): Observable<TransactionTreeOption[]> {
    return this.http.get<TransactionTreeOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/who-tree'));
  }

  getTagOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/tags'), {
      params: options,
      observe: 'response',
    });
  }

  getSplits(accountId: string, transactionId: string): Observable<TransactionSplitUpdate[]> {
    return this.http.get<TransactionSplitUpdate[]>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions/${transactionId}/splits`),
    );
  }

  update(accountId: string, transactionId: string, update: TransactionUpdate): Observable<FinancialTransaction> {
    return this.http.put<FinancialTransaction>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions/${transactionId}`),
      update,
    );
  }

  delete(accountId: string, transactionId: string): Observable<void> {
    return this.http.delete<void>(this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions/${transactionId}`));
  }

  create(accountId: string, update: TransactionUpdate): Observable<FinancialTransaction> {
    return this.http.post<FinancialTransaction>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions`),
      update,
    );
  }
}

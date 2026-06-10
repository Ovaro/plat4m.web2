import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOption } from 'app/core/request/request-util';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinancialTransaction } from '../finance.model';
import { Pagination } from 'app/core/request/request.model';
import { TransactionLookupQuery, TransactionOption, TransactionUpdate } from './transactions.types';

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

  getCategoryOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/categories'), {
      params: options,
      observe: 'response',
    });
  }

  getPayeeOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/payees'), {
      params: options,
      observe: 'response',
    });
  }

  getWhoOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/who'), {
      params: options,
      observe: 'response',
    });
  }

  getTagOptions(req: TransactionLookupQuery): Observable<HttpResponse<TransactionOption[]>> {
    const options = createRequestOption(req);
    return this.http.get<TransactionOption[]>(this.applicationConfigService.getEndpointFor('api/transactions/editor-options/tags'), {
      params: options,
      observe: 'response',
    });
  }

  update(accountId: string, transactionId: string, update: TransactionUpdate): Observable<FinancialTransaction> {
    return this.http.put<FinancialTransaction>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions/${transactionId}`),
      update,
    );
  }

  create(accountId: string, update: TransactionUpdate): Observable<FinancialTransaction> {
    return this.http.post<FinancialTransaction>(
      this.applicationConfigService.getEndpointFor(`api/account/${accountId}/transactions`),
      update,
    );
  }
}

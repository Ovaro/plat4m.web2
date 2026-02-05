import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOption } from 'app/core/request/request-util';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinancialTransaction } from '../finance.model';
import { Pagination } from 'app/core/request/request.model';

@Injectable({
  providedIn: 'root', // Add this line
})
export class Transactions {
  private resourceUrl = 'api/account/';
  private resourceUrlEnd = '/transactions-paging';

  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  get(accountId: string, req?: Pagination): Observable<HttpResponse<FinancialTransaction[]>> {
    const options = createRequestOption(req);
    return this.http.get<FinancialTransaction[]>(`${this.resourceUrl}${accountId}${this.resourceUrlEnd}`, {
      params: options,
      observe: 'response',
    });
    //this.applicationConfigService.getEndpointFor(this.resourceUrl)
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinancialAccount } from '../finance.model';

@Injectable({
  providedIn: 'root', // Add this line
})
export class AccountList {
  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  get(): Observable<FinancialAccount[]> {
    return this.http.get<FinancialAccount[]>(this.applicationConfigService.getEndpointFor('api/accounts/balances'));
  }

  getSimple(): Observable<FinancialAccount[]> {
    return this.http.get<FinancialAccount[]>(this.applicationConfigService.getEndpointFor('api/accounts'));
  }

  getByType(type: number): Observable<FinancialAccount[]> {
    return this.http.get<FinancialAccount[]>(this.applicationConfigService.getEndpointFor('api/accounts?type=' + String(type)));
  }
}

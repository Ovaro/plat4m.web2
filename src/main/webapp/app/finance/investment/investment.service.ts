import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinanceInvestmentSnapshotDetails, FinanceResourceSnapshots, InvestmentTransaction } from '../finance.model';

@Injectable({
  providedIn: 'root', // Add this line
})
export class InvestmentTransactions {
  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  getTransactions(id: string, includeClosedPositions: boolean): Observable<InvestmentTransaction[]> {
    if (includeClosedPositions) {
      return this.http.get<InvestmentTransaction[]>(
        this.applicationConfigService.getEndpointFor('api/investment/' + id + '/transactions?includeClosed=true'),
      );
    } else {
      return this.http.get<InvestmentTransaction[]>(
        this.applicationConfigService.getEndpointFor('api/investment/' + id + '/transactions?includeClosed=false'),
      );
    }
  }

  // getTransactionsPlus(id: string, includeClosedPositions: boolean): Observable<InvestmentDetails> {
  //     if(includeClosedPositions) {
  //         return this.http.get<InvestmentDetails>(this.applicationConfigService.getEndpointFor('api/investment/'+id+"/transactions-plus?includeClosed=true"));
  //     } else {
  //         return this.http.get<InvestmentDetails>(this.applicationConfigService.getEndpointFor('api/investment/'+id+"/transactions-plus?includeClosed=false"));
  //     }
  // }

  getSummary(id: string, includeClosedPositions: boolean): Observable<FinanceInvestmentSnapshotDetails> {
    if (includeClosedPositions) {
      return this.http.get<FinanceInvestmentSnapshotDetails>(
        this.applicationConfigService.getEndpointFor('api/investment/' + id + '/summary?includeClosed=true'),
      );
    } else {
      return this.http.get<FinanceInvestmentSnapshotDetails>(
        this.applicationConfigService.getEndpointFor('api/investment/' + id + '/summary?includeClosed=false'),
      );
    }
  }

  getHistory(userSecurityId: string, includeClosedPositions: boolean, periodAgo: string): Observable<FinanceResourceSnapshots[]> {
    return this.http.get<FinanceResourceSnapshots[]>(
      this.applicationConfigService.getEndpointFor(
        `api/portfolios/history?userSecurityId=${userSecurityId}&includeClosed=${includeClosedPositions}&periodAgo=${periodAgo}&numberOfPeriods=0`,
      ),
    );
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  FinanceInvestmentSnapshotDetails,
  FinanceInvestmentRollupIgnoreUpdate,
  FinanceLotGroup,
  FinanceResourceSnapshots,
  FinanceSecurityStoredPrice,
  FinanceSecurityStoredPriceUpdate,
  FinanceSecurityPriceRefreshResult,
  InvestmentTransaction,
} from '../finance.model';

@Injectable({
  providedIn: 'root', // Add this line
})
export class InvestmentTransactions {
  private static readonly HISTORY_PERIOD_COUNT = 365;

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

  updateRollupIgnore(
    id: string,
    includeClosedPositions: boolean,
    update: FinanceInvestmentRollupIgnoreUpdate,
  ): Observable<FinanceInvestmentSnapshotDetails> {
    return this.http.put<FinanceInvestmentSnapshotDetails>(
      this.applicationConfigService.getEndpointFor(`api/investment/${id}/rollup-ignore?includeClosed=${includeClosedPositions}`),
      update,
    );
  }

  getHistory(userSecurityId: string, includeClosedPositions: boolean, periodAgo: string): Observable<FinanceResourceSnapshots[]> {
    return this.http.get<FinanceResourceSnapshots[]>(
      this.applicationConfigService.getEndpointFor(
        `api/portfolios/history?userSecurityId=${userSecurityId}&includeClosed=${includeClosedPositions}&periodAgo=${periodAgo}&numberOfPeriods=${InvestmentTransactions.HISTORY_PERIOD_COUNT}`,
      ),
    );
  }

  refreshQuotes(userSecurityId: string): Observable<FinanceSecurityPriceRefreshResult> {
    return this.http.post<FinanceSecurityPriceRefreshResult>(this.applicationConfigService.getEndpointFor('api/security-prices/refresh'), {
      userSecurityId,
    });
  }

  getStoredPrices(userSecurityId: string): Observable<FinanceSecurityStoredPrice[]> {
    return this.http.get<FinanceSecurityStoredPrice[]>(
      this.applicationConfigService.getEndpointFor(`api/investment/${userSecurityId}/prices`),
    );
  }

  createStoredPrice(userSecurityId: string, update: FinanceSecurityStoredPriceUpdate): Observable<FinanceSecurityStoredPrice> {
    return this.http.post<FinanceSecurityStoredPrice>(
      this.applicationConfigService.getEndpointFor(`api/investment/${userSecurityId}/prices`),
      update,
    );
  }

  updateStoredPrice(
    userSecurityId: string,
    priceId: string,
    update: FinanceSecurityStoredPriceUpdate,
  ): Observable<FinanceSecurityStoredPrice> {
    return this.http.put<FinanceSecurityStoredPrice>(
      this.applicationConfigService.getEndpointFor(`api/investment/${userSecurityId}/prices/${priceId}`),
      update,
    );
  }

  deleteStoredPrice(userSecurityId: string, priceId: string): Observable<void> {
    return this.http.delete<void>(this.applicationConfigService.getEndpointFor(`api/investment/${userSecurityId}/prices/${priceId}`));
  }

  getLots(userSecurityId: string): Observable<FinanceLotGroup[]> {
    return this.http.get<FinanceLotGroup[]>(this.applicationConfigService.getEndpointFor(`api/investment/${userSecurityId}/lots`));
  }
}

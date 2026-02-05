import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinanceResourceSnapshots, FinanceSecurityHolding, InvestmentPortfolioDetails } from '../finance.model';

@Injectable({
  providedIn: 'root', // Add this line
})
export class InvestmentPortfolio {
  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  // get(includeClosedPositions: boolean): Observable<FinanceSecurityHolding[]> {
  //     if(includeClosedPositions) {
  //         return this.http.get<FinanceSecurityHolding[]>(this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=true'));
  //     } else {
  //         return this.http.get<FinanceSecurityHolding[]>(this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=false'));
  //     }
  // }

  get(accountId: string | null, includeClosedPositions: boolean): Observable<FinanceSecurityHolding[]> {
    if (accountId != null && includeClosedPositions) {
      return this.http.get<FinanceSecurityHolding[]>(
        this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=true&accountId=' + accountId),
      );
    } else if (accountId != null && !includeClosedPositions) {
      return this.http.get<FinanceSecurityHolding[]>(
        this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=false&accountId=' + accountId),
      );
    } else if (includeClosedPositions) {
      return this.http.get<FinanceSecurityHolding[]>(this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=true'));
    } else {
      return this.http.get<FinanceSecurityHolding[]>(this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=false'));
    }
  }

  getSummaries(accountId: string | null, includeClosedPositions: boolean, periodAgo: string): Observable<InvestmentPortfolioDetails> {
    // if(includeClosedPositions) {
    //     return this.http.get<InvestmentPortfolioDetails>(this.applicationConfigService.getEndpointFor('api/investment/summaries?includeClosed=true'));
    // } else {
    //     return this.http.get<InvestmentPortfolioDetails>(this.applicationConfigService.getEndpointFor('api/investment/summaries?includeClosed=false'));
    // }

    if (accountId != null && includeClosedPositions) {
      return this.http.get<InvestmentPortfolioDetails>(
        this.applicationConfigService.getEndpointFor(
          'api/portfolios/summaries?includeClosed=true&accountId=' + accountId + '&periodAgo=' + periodAgo,
        ),
      );
    } else if (accountId != null && !includeClosedPositions) {
      return this.http.get<InvestmentPortfolioDetails>(
        this.applicationConfigService.getEndpointFor(
          'api/portfolios/summaries?includeClosed=false&accountId=' + accountId + '&periodAgo=' + periodAgo,
        ),
      );
    } else if (includeClosedPositions) {
      return this.http.get<InvestmentPortfolioDetails>(
        this.applicationConfigService.getEndpointFor('api/portfolios/summaries?includeClosed=true&periodAgo=' + periodAgo),
      );
    } else {
      return this.http.get<InvestmentPortfolioDetails>(
        this.applicationConfigService.getEndpointFor('api/portfolios/summaries?includeClosed=false&periodAgo=' + periodAgo),
      );
    }
  }

  getPortfolioHistory(
    accountId: string | null,
    includeClosedPositions: boolean,
    periodAgo: string,
  ): Observable<FinanceResourceSnapshots[]> {
    if (accountId != null && includeClosedPositions) {
      return this.http.get<FinanceResourceSnapshots[]>(
        this.applicationConfigService.getEndpointFor(
          'api/portfolios/history?includeClosed=true&accountId=' + accountId + '&periodAgo=' + periodAgo + '&numberOfPeriods=0',
        ),
      );
    } else if (accountId != null && !includeClosedPositions) {
      return this.http.get<FinanceResourceSnapshots[]>(
        this.applicationConfigService.getEndpointFor(
          'api/portfolios/history?includeClosed=false&accountId=' + accountId + '&periodAgo=' + periodAgo + '&numberOfPeriods=0',
        ),
      );
    } else if (includeClosedPositions) {
      return this.http.get<FinanceResourceSnapshots[]>(
        this.applicationConfigService.getEndpointFor(
          'api/portfolios/history?includeClosed=true&periodAgo=' + periodAgo + '&numberOfPeriods=0',
        ),
      );
    } else {
      return this.http.get<FinanceResourceSnapshots[]>(
        this.applicationConfigService.getEndpointFor(
          'api/portfolios/history?includeClosed=false&periodAgo=' + periodAgo + '&numberOfPeriods=0',
        ),
      );
    }
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  PortfolioTrade,
  CustomPortfolio,
  CustomPortfolioOptions,
  FinanceResourceSnapshots,
  FinanceSecurityHolding,
  FinanceSecurityPriceRefreshResult,
  InvestmentPortfolioDetails,
} from '../finance.model';

@Injectable({
  providedIn: 'root', // Add this line
})
export class InvestmentPortfolio {
  private activeQuoteRefreshResult: FinanceSecurityPriceRefreshResult | null = null;

  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  getActiveQuoteRefreshResult(): FinanceSecurityPriceRefreshResult | null {
    return this.activeQuoteRefreshResult;
  }

  setActiveQuoteRefreshResult(result: FinanceSecurityPriceRefreshResult | null): void {
    this.activeQuoteRefreshResult = result;
  }

  clearActiveQuoteRefreshResult(): void {
    this.activeQuoteRefreshResult = null;
  }

  // get(includeClosedPositions: boolean): Observable<FinanceSecurityHolding[]> {
  //     if(includeClosedPositions) {
  //         return this.http.get<FinanceSecurityHolding[]>(this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=true'));
  //     } else {
  //         return this.http.get<FinanceSecurityHolding[]>(this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=false'));
  //     }
  // }

  getCustomPortfolios(): Observable<CustomPortfolio[]> {
    return this.http.get<CustomPortfolio[]>(this.applicationConfigService.getEndpointFor('api/custom-portfolios'));
  }

  getCustomPortfolioOptions(): Observable<CustomPortfolioOptions> {
    return this.http.get<CustomPortfolioOptions>(this.applicationConfigService.getEndpointFor('api/custom-portfolios/options'));
  }

  createCustomPortfolio(portfolio: CustomPortfolio): Observable<CustomPortfolio> {
    return this.http.post<CustomPortfolio>(this.applicationConfigService.getEndpointFor('api/custom-portfolios'), portfolio);
  }

  updateCustomPortfolio(portfolio: CustomPortfolio): Observable<CustomPortfolio> {
    return this.http.put<CustomPortfolio>(this.applicationConfigService.getEndpointFor(`api/custom-portfolios/${portfolio.id}`), portfolio);
  }

  deleteCustomPortfolio(portfolioId: string): Observable<void> {
    return this.http.delete<void>(this.applicationConfigService.getEndpointFor(`api/custom-portfolios/${portfolioId}`));
  }

  get(
    accountId: string | null,
    includeClosedPositions: boolean,
    customPortfolioId: string | null = null,
    periodAgo = '',
  ): Observable<FinanceSecurityHolding[]> {
    return this.http.get<FinanceSecurityHolding[]>(this.applicationConfigService.getEndpointFor('api/portfolios'), {
      params: this.buildPortfolioParams(accountId, includeClosedPositions, periodAgo, customPortfolioId),
    });
  }

  getSummaries(
    accountId: string | null,
    includeClosedPositions: boolean,
    periodAgo: string,
    customPortfolioId: string | null = null,
  ): Observable<InvestmentPortfolioDetails> {
    return this.http.get<InvestmentPortfolioDetails>(this.applicationConfigService.getEndpointFor('api/portfolios/summaries'), {
      params: this.buildPortfolioParams(accountId, includeClosedPositions, periodAgo, customPortfolioId),
    });
  }

  getPortfolioHistory(
    accountId: string | null,
    includeClosedPositions: boolean,
    periodAgo: string,
    customPortfolioId: string | null = null,
  ): Observable<FinanceResourceSnapshots[]> {
    return this.http.get<FinanceResourceSnapshots[]>(this.applicationConfigService.getEndpointFor('api/portfolios/history'), {
      params: this.buildPortfolioParams(accountId, includeClosedPositions, periodAgo, customPortfolioId).set('numberOfPeriods', '0'),
    });
  }

  getTrades(
    accountId: string | null,
    includeClosedPositions: boolean,
    customPortfolioId: string | null = null,
  ): Observable<PortfolioTrade[]> {
    return this.http.get<PortfolioTrade[]>(this.applicationConfigService.getEndpointFor('api/portfolios/trades'), {
      params: this.buildPortfolioParams(accountId, includeClosedPositions, '', customPortfolioId),
    });
  }

  refreshQuotes(accountId: string | null): Observable<FinanceSecurityPriceRefreshResult> {
    return this.http.post<FinanceSecurityPriceRefreshResult>(
      this.applicationConfigService.getEndpointFor('api/security-prices/refresh/start'),
      { accountId },
    );
  }

  getRefreshQuoteStatus(jobId: string): Observable<FinanceSecurityPriceRefreshResult> {
    return this.http.get<FinanceSecurityPriceRefreshResult>(
      this.applicationConfigService.getEndpointFor(`api/security-prices/refresh/${jobId}`),
    );
  }

  updateRefreshQuoteSelection(jobId: string, userSecurityId: string, selected: boolean): Observable<FinanceSecurityPriceRefreshResult> {
    return this.http.post<FinanceSecurityPriceRefreshResult>(
      this.applicationConfigService.getEndpointFor(
        `api/security-prices/refresh/${jobId}/items/${userSecurityId}/selection?selected=${selected}`,
      ),
      {},
    );
  }

  applyRefreshQuotes(jobId: string): Observable<FinanceSecurityPriceRefreshResult> {
    return this.http.post<FinanceSecurityPriceRefreshResult>(
      this.applicationConfigService.getEndpointFor(`api/security-prices/refresh/${jobId}/apply`),
      {},
    );
  }

  refreshQuotesSync(accountId: string | null): Observable<FinanceSecurityPriceRefreshResult> {
    return this.http.post<FinanceSecurityPriceRefreshResult>(this.applicationConfigService.getEndpointFor('api/security-prices/refresh'), {
      accountId,
    });
  }

  private buildPortfolioParams(
    accountId: string | null,
    includeClosedPositions: boolean,
    periodAgo: string,
    customPortfolioId: string | null,
  ): HttpParams {
    let params = new HttpParams().set('includeClosed', String(includeClosedPositions));
    if (periodAgo !== null && periodAgo !== undefined) {
      params = params.set('periodAgo', periodAgo);
    }
    if (customPortfolioId) {
      return params.set('customPortfolioId', customPortfolioId);
    }
    if (accountId) {
      return params.set('accountId', accountId);
    }
    return params;
  }
}

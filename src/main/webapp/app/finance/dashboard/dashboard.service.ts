import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import {
  FinanceIndicators,
  FinanceNestedResource,
  FinanceResourceSnapshots,
  FinanceSecurityHolding,
  FinancialAccount,
  SnapshotWithDelta,
} from '../finance.model';

@Injectable({
  providedIn: 'root', // Add this line
})
export class DashboardService {
  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  getSnapshot(type: string, classId: string, periodAgo: string): Observable<SnapshotWithDelta> {
    if (periodAgo) {
      return this.http.get<SnapshotWithDelta>(
        this.applicationConfigService.getEndpointFor(
          'api/snapshot-processor?type=' + type + '&classId=' + classId + '&periodAgo=' + periodAgo,
        ),
      );
    } else {
      return this.http.get<SnapshotWithDelta>(
        this.applicationConfigService.getEndpointFor('api/snapshot-processor?type=' + type + '&classId=' + classId),
      );
    }
  }

  getAccountIndicators(periodAgo: string): Observable<FinanceIndicators> {
    return this.http.get<FinanceIndicators>(this.applicationConfigService.getEndpointFor('api/accounts/indicators?periodAgo=' + periodAgo));
  }

  getPortfolioIndicators(periodAgo: string): Observable<FinanceIndicators> {
    return this.http.get<FinanceIndicators>(
      this.applicationConfigService.getEndpointFor('api/portfolios/indicators?periodAgo=' + periodAgo),
    );
  }

  getAccountSnapshots(periodAgo: string): Observable<FinanceResourceSnapshots[]> {
    return this.http.get<FinanceResourceSnapshots[]>(
      this.applicationConfigService.getEndpointFor('api/accounts/history?periodAgo=' + periodAgo),
    );
  }

  getPortfolioSnapshots(includeClosedPositions: boolean, periodAgo: string): Observable<FinanceResourceSnapshots[]> {
    if (includeClosedPositions) {
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

  getSecurityHoldingIndicators(includeClosedPositions: boolean, periodAgo: string): Observable<FinanceSecurityHolding[]> {
    if (includeClosedPositions) {
      return this.http.get<FinanceSecurityHolding[]>(
        this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=true&periodAgo=' + periodAgo),
      );
    } else {
      return this.http.get<FinanceSecurityHolding[]>(
        this.applicationConfigService.getEndpointFor('api/portfolios?includeClosed=false&periodAgo=' + periodAgo),
      );
    }
  }

  getResources(): Observable<FinanceNestedResource[]> {
    return this.http.get<FinanceNestedResource[]>(this.applicationConfigService.getEndpointFor('api/resources'));
  }
}

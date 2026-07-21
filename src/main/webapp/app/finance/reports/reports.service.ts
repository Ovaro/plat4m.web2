import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinanceLotView } from '../finance.model';
import { ReportConfig, ReportDefinition, ReportDrilldownRequest, ReportDrilldownResult, ReportResult } from './reports.types';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  getDefinitions(): Observable<ReportDefinition[]> {
    return this.http.get<ReportDefinition[]>(this.applicationConfigService.getEndpointFor('api/reports'));
  }

  getConfigs(reportKey: string): Observable<ReportConfig[]> {
    return this.http.get<ReportConfig[]>(this.applicationConfigService.getEndpointFor('api/reports/configs'), {
      params: { reportKey },
    });
  }

  createConfig(config: ReportConfig): Observable<ReportConfig> {
    return this.http.post<ReportConfig>(this.applicationConfigService.getEndpointFor('api/reports/configs'), config);
  }

  updateConfig(config: ReportConfig): Observable<ReportConfig> {
    return this.http.put<ReportConfig>(this.applicationConfigService.getEndpointFor(`api/reports/configs/${config.id}`), config);
  }

  runIncomeExpenses(config: ReportConfig): Observable<ReportResult> {
    return this.http.post<ReportResult>(this.applicationConfigService.getEndpointFor('api/reports/income-expenses/run'), config);
  }

  getIncomeExpenseDrilldown(request: ReportDrilldownRequest): Observable<ReportDrilldownResult> {
    return this.http.post<ReportDrilldownResult>(
      this.applicationConfigService.getEndpointFor('api/reports/income-expenses/drilldown'),
      request,
    );
  }

  getIncomeExpenseTransactionLots(transactionId: string): Observable<FinanceLotView[]> {
    return this.http.get<FinanceLotView[]>(
      this.applicationConfigService.getEndpointFor(`api/reports/income-expenses/transactions/${transactionId}/lots`),
    );
  }
}

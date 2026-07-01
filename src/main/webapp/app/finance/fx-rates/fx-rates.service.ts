import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinanceFXImportRequest, FinanceFXImportResult, FinanceFXRate, FinanceFXRateUpdate } from './fx-rates.types';

@Injectable({ providedIn: 'root' })
export class FxRatesService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  getRates(): Observable<FinanceFXRate[]> {
    return this.http.get<FinanceFXRate[]>(this.applicationConfigService.getEndpointFor('api/fx'));
  }

  getRateHistory(fromIsoCode: string, toIsoCode: string): Observable<FinanceFXRate[]> {
    return this.http.get<FinanceFXRate[]>(this.applicationConfigService.getEndpointFor('api/fx/history'), {
      params: {
        from: fromIsoCode,
        to: toIsoCode,
      },
    });
  }

  createRate(update: FinanceFXRateUpdate): Observable<FinanceFXRate> {
    return this.http.post<FinanceFXRate>(this.applicationConfigService.getEndpointFor('api/fx'), update);
  }

  updateRate(id: string, update: FinanceFXRateUpdate): Observable<FinanceFXRate> {
    return this.http.put<FinanceFXRate>(this.applicationConfigService.getEndpointFor(`api/fx/${id}`), update);
  }

  updateFavourite(id: string, favourite: boolean): Observable<FinanceFXRate> {
    return this.http.put<FinanceFXRate>(this.applicationConfigService.getEndpointFor(`api/fx/${id}/favourite`), { favourite });
  }

  deleteRate(id: string): Observable<void> {
    return this.http.delete<void>(this.applicationConfigService.getEndpointFor(`api/fx/${id}`));
  }

  importFrankfurterRates(request: FinanceFXImportRequest): Observable<FinanceFXImportResult> {
    return this.http.post<FinanceFXImportResult>(this.applicationConfigService.getEndpointFor('api/fx/import/frankfurter'), request);
  }
}

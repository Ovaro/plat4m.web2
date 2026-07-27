import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { DemoDataBatch, DemoDataDeleteResult, DemoDataOptions, DemoDataRequest, DemoDataResult } from './demo-data.types';

@Injectable({ providedIn: 'root' })
export class DemoDataService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/demo-data');

  options(): Observable<DemoDataOptions> {
    return this.http.get<DemoDataOptions>(`${this.resourceUrl}/options`);
  }

  batches(): Observable<DemoDataBatch[]> {
    return this.http.get<DemoDataBatch[]>(`${this.resourceUrl}/batches`);
  }

  preview(request: DemoDataRequest): Observable<DemoDataResult> {
    return this.http.post<DemoDataResult>(`${this.resourceUrl}/preview`, request);
  }

  generate(request: DemoDataRequest): Observable<DemoDataResult> {
    return this.http.post<DemoDataResult>(`${this.resourceUrl}/generate`, request);
  }

  deleteBatch(batchId: string): Observable<DemoDataDeleteResult> {
    return this.http.delete<DemoDataDeleteResult>(`${this.resourceUrl}/batches/${batchId}`);
  }
}

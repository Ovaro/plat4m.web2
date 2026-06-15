import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { AiAssistantRequest, AiAssistantResponse } from './ai-assistant.types';

@Injectable({ providedIn: 'root' })
export class AiAssistantService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/ai-assistant');

  query(request: AiAssistantRequest): Observable<AiAssistantResponse> {
    return this.http.post<AiAssistantResponse>(`${this.resourceUrl}/query`, request);
  }
}

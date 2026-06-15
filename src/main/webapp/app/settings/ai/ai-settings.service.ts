import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { AiModelOption } from './ai-model-option.model';
import { AiSettings, AiSettingsUpdateRequest } from './ai-settings.types';

@Injectable({ providedIn: 'root' })
export class AiSettingsService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/account/ai-settings');

  getSettings(): Observable<AiSettings> {
    return this.http.get<AiSettings>(this.resourceUrl);
  }

  getModels(): Observable<AiModelOption[]> {
    return this.http.get<AiModelOption[]>(`${this.resourceUrl}/models`);
  }

  updateSettings(request: AiSettingsUpdateRequest): Observable<AiSettings> {
    return this.http.post<AiSettings>(this.resourceUrl, request);
  }
}

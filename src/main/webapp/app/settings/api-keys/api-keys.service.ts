import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { CreateUserApiKeyRequest, CreatedUserApiKeyResponse, UserApiKey } from './api-keys.types';

@Injectable({ providedIn: 'root' })
export class ApiKeysService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/account/api-keys');

  list(): Observable<UserApiKey[]> {
    return this.http.get<UserApiKey[]>(this.resourceUrl);
  }

  create(request: CreateUserApiKeyRequest): Observable<CreatedUserApiKeyResponse> {
    return this.http.post<CreatedUserApiKeyResponse>(this.resourceUrl, request);
  }

  remove(id: string): Observable<null> {
    return this.http.delete<null>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }
}

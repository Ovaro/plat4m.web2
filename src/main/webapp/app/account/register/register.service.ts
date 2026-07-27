import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';

import { Registration } from './register.model';

export type RegistrationResponse = {
  activationEmailSent: boolean;
  message: string;
};

@Injectable({ providedIn: 'root' })
export class RegisterService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  save(registration: Registration): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(this.applicationConfigService.getEndpointFor('api/register'), registration);
  }
}

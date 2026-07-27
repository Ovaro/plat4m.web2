import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';

@Injectable({ providedIn: 'root' })
export class Register {
  private resourceUrl = 'api/';

  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  save(account: any): Observable<{ activationEmailSent: boolean; message: string }> {
    return this.http.post<{ activationEmailSent: boolean; message: string }>(
      this.applicationConfigService.getEndpointFor(`${this.resourceUrl}register`),
      account,
    );
  }
}

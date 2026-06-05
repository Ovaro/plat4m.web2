import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { Observable, map, mergeMap, of, throwError } from 'rxjs';

import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { AuthServerProvider } from 'app/core/auth/auth-jwt.service';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

import { Login } from './login.model';

@Injectable({ providedIn: 'root' })
export class LoginService {
  private readonly accountService = inject(AccountService);
  private readonly authServerProvider = inject(AuthServerProvider);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly http = inject(HttpClient);

  login(credentials: Login): Observable<Account | null> {
    return this.authServerProvider.login(credentials).pipe(mergeMap(() => this.accountService.identity(true)));
  }

  checkExists(username: string): Observable<void> {
    return this.http
      .get<Array<Partial<Account>>>(this.applicationConfigService.getEndpointFor('api/users?sort=login,asc&page=0&size=1000'))
      .pipe(
        map(users =>
          users.some(user => user.email?.toLowerCase() === username.toLowerCase() || user.login?.toLowerCase() === username.toLowerCase()),
        ),
        mergeMap(exists => (exists ? of(void 0) : throwError(() => new HttpErrorResponse({ status: 404, statusText: 'Not Found' })))),
      );
  }

  logout(): void {
    this.authServerProvider.logout().subscribe({ complete: () => this.accountService.authenticate(null) });
  }
}

import { inject, isDevMode } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs';

import { AccountService } from 'app/core/auth/account.service';

import { StateStorageService } from './state-storage.service';

export const UserRouteAccessService: CanActivateFn = (next: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const accountService = inject(AccountService);
  const router = inject(Router);
  const stateStorageService = inject(StateStorageService);

  return accountService.identity().pipe(
    map(account => {
      if (account) {
        const { authorities } = next.data;

        if (!authorities || authorities.length === 0 || accountService.hasAnyAuthority(authorities)) {
          return true;
        }

        if (isDevMode()) {
          console.error('User does not have any of the required authorities:', authorities);
        }
        router.navigate(['accessdenied']);
        return false;
      } // OVARO MOD ::LOGIN:: START - handle not logged in
      else {
        // console.log('No account found');
        if (state.url === '/') {
          // No account but going to home... Redirect to public
          // console.log('UserRouteAccessService::Redirecting to public....');
          router.navigate(['/login'], { skipLocationChange: true });
          return true;
        }
      }
      // OVARO MOD ::LOGIN:: END - handle not logged in

      stateStorageService.storeUrl(state.url);
      // OVARO MOD ::LOGIN:: - handle not logged in (2)
      router.navigate(['accessdenied']).then(() => {
        // only show the login dialog, if the user hasn't logged in yet
        // if (!account) {
        //     this.loginModalService.open();
        // }
        router.navigate(['/'], { skipLocationChange: true });
      });
      // OVARO MOD ::LOGIN:: - handle not logged in (2)
      //router.navigate(['/login']);
      return false;
    }),
  );
};

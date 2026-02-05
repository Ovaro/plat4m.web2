import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { AccountListComponent } from './account-list.component';

export const accountListRoute: Route = {
  path: 'account-list',
  component: AccountListComponent,
  data: {
    pageTitle: 'account-list.title',
  },
  canActivate: [UserRouteAccessService],
};

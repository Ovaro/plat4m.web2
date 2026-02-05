import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { TransactionsComponent } from './transactions.component';

export const transactionsRoute: Route = {
  path: 'transactions/:id',
  component: TransactionsComponent,
  data: {
    pageTitle: 'transactions.title',
  },
  canActivate: [UserRouteAccessService],
};

export const transactionsBaseRoute: Route = {
  path: 'transactions',
  component: TransactionsComponent,
  data: {
    pageTitle: 'transactions.title',
  },
  canActivate: [UserRouteAccessService],
};

import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { InvestmentComponent } from './investment.component';

export const investmentRoute: Route = {
  path: 'investment/:id',
  component: InvestmentComponent,
  data: {
    pageTitle: 'investment-holding.title',
  },
  canActivate: [UserRouteAccessService],
};

export const investmentBaseRoute: Route = {
  path: 'investment',
  component: InvestmentComponent,
  data: {
    pageTitle: 'investment.title',
  },
  canActivate: [UserRouteAccessService],
};

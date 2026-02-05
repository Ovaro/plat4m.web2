import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { InvestmentPortfolioComponent } from './investment-portfolio.component';

export const investmentPortfolioRoute: Route = {
  path: 'portfolio/:id',
  component: InvestmentPortfolioComponent,
  data: {
    pageTitle: 'investment-portfolio.title',
  },
  canActivate: [UserRouteAccessService],
};

export const investmentPortfolioBaseRoute: Route = {
  path: 'portfolio',
  component: InvestmentPortfolioComponent,
  data: {
    pageTitle: 'investment-portfolio.title',
  },
  canActivate: [UserRouteAccessService],
};

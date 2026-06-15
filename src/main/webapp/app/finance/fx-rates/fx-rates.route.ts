import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { FxRatesComponent } from './fx-rates.component';

export const fxRatesRoute: Route = {
  path: 'fx-rates',
  component: FxRatesComponent,
  data: {
    pageTitle: 'FX Rates',
  },
  canActivate: [UserRouteAccessService],
};

import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { PayeesComponent } from './payees.component';

export const payeesRoute: Route = {
  path: 'payees',
  component: PayeesComponent,
  data: {
    pageTitle: 'Payees',
  },
  canActivate: [UserRouteAccessService],
};

import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { ReportsHomeComponent } from './reports-home.component';

export const reportsHomeRoute: Route = {
  path: 'reports',
  component: ReportsHomeComponent,
  data: {
    pageTitle: 'Reports',
  },
  canActivate: [UserRouteAccessService],
};

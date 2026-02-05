import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { DashboardComponent } from './dashboard.component';

export const dashboardRoute: Route = {
  path: 'dashboard',
  component: DashboardComponent,
  data: {
    pageTitle: 'dashboard.title',
  },
  canActivate: [UserRouteAccessService],
};

import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { PlannerComponent } from './planner.component';

export const plannerRoute: Route = {
  path: 'planner',
  component: PlannerComponent,
  data: {
    pageTitle: 'planner.title',
  },
  canActivate: [UserRouteAccessService],
};

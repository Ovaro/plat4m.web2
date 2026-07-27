import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { DemoDataComponent } from './demo-data.component';

export const demoDataRoute: Route = {
  path: 'demo-data',
  component: DemoDataComponent,
  data: {
    pageTitle: 'Generate Demo Data',
  },
  canActivate: [UserRouteAccessService],
};

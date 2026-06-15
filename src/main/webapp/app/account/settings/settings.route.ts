import { Route } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const settingsRoute: Route = {
  path: 'settings',
  redirectTo: '/settings/general',
  pathMatch: 'full',
  canActivate: [UserRouteAccessService],
};

export default settingsRoute;

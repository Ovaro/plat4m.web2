import { Route } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ImportComponent } from './import.component';

export const importRoute: Route = {
  path: 'import',
  component: ImportComponent,
  data: {
    pageTitle: 'import.title',
  },
  canActivate: [UserRouteAccessService],
};

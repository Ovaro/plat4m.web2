import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { CategoriesComponent } from './categories.component';

export const categoriesRoute: Route = {
  path: 'categories',
  component: CategoriesComponent,
  data: {
    pageTitle: 'Categories',
  },
  canActivate: [UserRouteAccessService],
};

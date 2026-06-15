import { Route } from '@angular/router';

import NavbarComponent from './navbar';

export const navbarRoute: Route = {
  path: '',
  component: NavbarComponent,
  outlet: 'navbar',
};

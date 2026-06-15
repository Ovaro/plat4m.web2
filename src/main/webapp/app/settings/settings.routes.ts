import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { ApiKeysComponent } from './api-keys/api-keys.component';
import { AiSettingsComponent } from './ai/ai-settings.component';
import { SettingsGeneralComponent } from './general/settings-general.component';
import { SettingsShellComponent } from './settings-shell.component';

const settingsRoutes: Routes = [
  {
    path: '',
    component: SettingsShellComponent,
    canActivate: [UserRouteAccessService],
    data: {
      pageTitle: 'settings.shell.title',
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'general',
      },
      {
        path: 'general',
        component: SettingsGeneralComponent,
        data: {
          pageTitle: 'settings.general.title',
        },
      },
      {
        path: 'api-keys',
        component: ApiKeysComponent,
        data: {
          pageTitle: 'API Keys',
        },
      },
      {
        path: 'ai',
        component: AiSettingsComponent,
        data: {
          pageTitle: 'AI',
        },
      },
    ],
  },
];

export default settingsRoutes;

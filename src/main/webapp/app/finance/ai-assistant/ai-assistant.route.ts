import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { AiAssistantComponent } from './ai-assistant.component';

export const aiAssistantRoute: Route = {
  path: 'assistant',
  component: AiAssistantComponent,
  data: {
    pageTitle: 'AI Assistant',
  },
  canActivate: [UserRouteAccessService],
};

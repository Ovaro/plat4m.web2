import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID, DEFAULT_CURRENCY_CODE, importProvidersFrom, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import {
  NavigationError,
  Router,
  RouterFeatures,
  TitleStrategy,
  provideRouter,
  withComponentInputBinding,
  withDebugTracing,
  withNavigationErrorHandler,
} from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { NgbDateAdapter } from '@ng-bootstrap/ng-bootstrap';
import { environment } from 'environments/environment';

import { authExpiredInterceptor } from 'app/core/interceptor/auth-expired.interceptor';
import { authInterceptor } from 'app/core/interceptor/auth.interceptor';
import { errorHandlerInterceptor } from 'app/core/interceptor/error-handler.interceptor';
import { notificationInterceptor } from 'app/core/interceptor/notification.interceptor';

import './config/dayjs';
import { TranslationModule } from 'app/shared/language/translation.module';

import { AppPageTitleStrategy } from './app-page-title-strategy';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { CookieModule } from 'ngx-cookie';
import routes from './app.routes';
import { NgbDateDayjsAdapter } from './config/datepicker-adapter';

const routerFeatures: RouterFeatures[] = [
  withComponentInputBinding(),
  withNavigationErrorHandler((e: NavigationError) => {
    const router = inject(Router);
    if (e.error.status === 403) {
      router.navigate(['/accessdenied']);
    } else if (e.error.status === 404) {
      router.navigate(['/404']);
    } else if (e.error.status === 401) {
      router.navigate(['/login']);
    } else {
      router.navigate(['/error']);
    }
  }),
];
if (environment.DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}

export const appConfig: ApplicationConfig = {
  providers: [
    importProvidersFrom(CookieModule.withOptions()),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.theme-dark',
        },
      },
    }),
    provideRouter(routes, ...routerFeatures),
    // Set this to true to enable service worker (PWA)
    provideServiceWorker('ngsw-worker.js', { enabled: false }),
    importProvidersFrom(TranslationModule),
    provideHttpClient(withInterceptors([authInterceptor, authExpiredInterceptor, errorHandlerInterceptor, notificationInterceptor])),
    Title,
    { provide: LOCALE_ID, useValue: 'en-AU' },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'AUD' },
    { provide: NgbDateAdapter, useClass: NgbDateDayjsAdapter },
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
  ],
};

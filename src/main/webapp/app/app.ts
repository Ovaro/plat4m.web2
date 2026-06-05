import { registerLocaleData } from '@angular/common';
import locale from '@angular/common/locales/en';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import dayjs from 'dayjs/esm';

import { ApplicationConfigService } from 'app/core/config/application-config.service';

import { fontAwesomeIcons } from './config/font-awesome-icons';
import { TrackerService } from './core/tracker/tracker.service';
import Main from './layouts/main/main';

import { AllCommunityModule, ClientSideRowModelModule, InfiniteRowModelModule, ModuleRegistry } from 'ag-grid-community';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule, InfiniteRowModelModule]);

@Component({
  selector: 'jhi-app',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<jhi-main />',
  imports: [Main],
})
export default class App {
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly iconLibrary = inject(FaIconLibrary);
  private readonly dpConfig = inject(NgbDatepickerConfig);
  private readonly trackerService = inject(TrackerService);

  constructor() {
    this.trackerService.setup();
    this.applicationConfigService.setEndpointPrefix(SERVER_API_URL);
    registerLocaleData(locale);
    this.iconLibrary.addIcons(...fontAwesomeIcons);
    // ZZ: Upgrade here.
    // this.dpConfig.minDate = { year: dayjs().subtract(100, 'year').year(), month: 1, day: 1 };
  }
}

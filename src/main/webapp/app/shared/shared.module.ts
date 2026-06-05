import { NgModule } from '@angular/core';

import { Alert as AlertComponent } from './alert/alert';
import { AlertError as AlertErrorComponent } from './alert/alert-error';
// import { HasAnyAuthorityDirective } from './auth/has-any-authority.directive';
// import { DurationPipe } from './date/duration.pipe';
// import { FormatMediumDatetimePipe } from './date/format-medium-datetime.pipe';
// import { FormatMediumDatePipe } from './date/format-medium-date.pipe';
import { SortByDirective } from './sort/sort-by.directive';
import { SortDirective } from './sort/sort.directive';
// import { ItemCountComponent } from './pagination/item-count.component';
import { DeltaValueComponent } from './delta-value/delta-value.component';
// import { FilterComponent } from './filter/filter.component';

import { CommonModule } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslateModule } from '@ngx-translate/core';

import FindLanguageFromKeyPipe from './language/find-language-from-key.pipe';
import TranslateDirective from './language/translate.directive';
import { Tooltip, TooltipModule } from 'primeng/tooltip';
import { SelectButton, SelectButtonModule } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

@NgModule({
  imports: [AlertComponent, AlertErrorComponent, FindLanguageFromKeyPipe, TranslateDirective, DeltaValueComponent],
  // declarations: [
  //   FindLanguageFromKeyPipe,
  //   TranslateDirective,
  //   AlertComponent,
  //   AlertErrorComponent,clear
  //   // HasAnyAuthorityDirective,
  //   // DurationPipe,
  //   // FormatMediumDatetimePipe,
  //   // FormatMediumDatePipe,
  //   SortByDirective,
  //   SortDirective,
  //   // ItemCountComponent,
  //   DeltaValueComponent,
  //   // FilterComponent,
  // ],
  exports: [
    // OLD
    // HasAnyAuthorityDirective,
    // DurationPipe,
    // FormatMediumDatetimePipe,
    // FormatMediumDatePipe,
    // SortByDirective,
    // SortDirective,
    // // ItemCountComponent,
    // DeltaValueComponent,
    // FilterComponent,
    // NEW
    CommonModule,
    NgbModule,
    FontAwesomeModule,
    AlertComponent,
    AlertErrorComponent,
    TranslateModule,
    FindLanguageFromKeyPipe,
    TranslateDirective,
    TooltipModule,
    FormsModule,
    SelectButtonModule,
    SelectModule,
    ToggleSwitchModule,
    DeltaValueComponent,
    SkeletonModule,
  ],
})
export default class SharedModule {}

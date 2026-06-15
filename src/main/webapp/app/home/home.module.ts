import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import Home from './home';

@NgModule({
  imports: [SharedModule, Home, RouterModule.forChild([{ path: '', component: Home }])],
})
export class HomeModule {}

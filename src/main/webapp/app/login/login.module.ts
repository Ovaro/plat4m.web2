import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import LoginComponent from './login';

import { SelectButtonModule } from 'primeng/selectbutton';

@NgModule({
  imports: [SharedModule, SelectButtonModule, LoginComponent, RouterModule.forChild([{ path: '', component: LoginComponent }])],
})
export class LoginModule {}

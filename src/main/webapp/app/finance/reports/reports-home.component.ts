import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';

@Component({
  selector: 'jhi-reports-home',
  templateUrl: './reports-home.component.html',
  styleUrls: ['./reports-home.component.scss'],
  imports: [SharedModule, RouterModule],
})
export class ReportsHomeComponent {}

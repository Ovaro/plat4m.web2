import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AccountListComponent } from './account-list/account-list.component';
import { InvestmentPortfolioComponent } from './investment-portfolio/investment-portfolio.component';
import { RouterModule } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ParentDynamicComponent } from './dashboard/parentDynamic.component';
import { PlannerComponent } from './planner/planner.component';
import { TransactionsComponent } from './transactions/transactions.component';
import { ImportComponent } from './import/import.component';
import SharedModule from 'app/shared/shared.module';

import { AccountList } from './account-list/account-list.service';
import { Transactions } from './transactions/transactions.service';
import { InvestmentPortfolio } from './investment-portfolio/investment-portfolio.service';
import { DashboardService } from './dashboard/dashboard.service';

import { AgGridModule } from 'ag-grid-angular';
import { NgApexchartsModule } from 'ng-apexcharts';

import { CurrencyPipe } from '@angular/common';

import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputSwitchModule } from 'primeng/inputswitch';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';
import { GridsterModule } from 'angular-gridster2';
import { InvestmentComponent } from './investment/investment.component';
import { InvestmentTransactions } from './investment/investment.service';
import { FileUploadModule } from '@iplab/ngx-file-upload';
import { SplitterModule } from 'primeng/splitter';

@NgModule({
  declarations: [
    AccountListComponent,
    DashboardComponent,
    PlannerComponent,
    TransactionsComponent,
    InvestmentPortfolioComponent,
    InvestmentComponent,
    ImportComponent,
  ],
  imports: [
    SharedModule,
    CommonModule,
    AgGridModule,
    DropdownModule,
    NgApexchartsModule,
    SelectButtonModule,
    TooltipModule,
    InputSwitchModule,
    SkeletonModule,
    FileUploadModule,
    NgApexchartsModule,
    GridsterModule,
    SplitterModule,
    ParentDynamicComponent,
  ],
  providers: [AccountList, Transactions, InvestmentPortfolio, DashboardService, CurrencyPipe, InvestmentTransactions],
})
export class FinanceModule {}

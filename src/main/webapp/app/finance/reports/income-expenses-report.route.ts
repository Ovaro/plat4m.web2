import { Route } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { IncomeExpensesReportComponent } from './income-expenses-report.component';

export const incomeExpensesReportRoute: Route = {
  path: 'reports/income-expenses',
  component: IncomeExpensesReportComponent,
  data: {
    pageTitle: 'Income & Expenses',
    reportVariant: 'income-expenses',
  },
  canActivate: [UserRouteAccessService],
};

export const capitalGainsReportRoute: Route = {
  path: 'reports/capital-gains',
  component: IncomeExpensesReportComponent,
  data: {
    pageTitle: 'Capital Gains',
    reportVariant: 'capital-gains',
  },
  canActivate: [UserRouteAccessService],
};

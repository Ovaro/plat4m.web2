import { Routes } from '@angular/router';

import { accountListRoute } from './account-list/account-list.route';
import { dashboardRoute } from './dashboard/dashboard.route';
import { plannerRoute } from './planner/planner.route';
import { transactionsBaseRoute, transactionsRoute } from './transactions/transactions.route';
import { investmentPortfolioBaseRoute, investmentPortfolioRoute } from './investment-portfolio/investment-portfolio.route';
import { investmentBaseRoute, investmentRoute } from './investment/investment.route';
import { importRoute } from './import/import.route';

const routes: Routes = [
  accountListRoute,
  plannerRoute,
  dashboardRoute,
  transactionsBaseRoute,
  transactionsRoute,
  investmentPortfolioBaseRoute,
  investmentPortfolioRoute,
  investmentBaseRoute,
  investmentRoute,
  importRoute,
];

export default routes;
// const routes: Routes =  FINANCE_ROUTES;

// export const financeState: Routes = [
//   {
//     path: '',
//     children: FINANCE_ROUTES
//   },
// ];

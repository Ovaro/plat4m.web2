import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterModule } from '@angular/router';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { FinancialAccount } from '../finance.model';
import { AccountList } from './account-list.service';
import SharedModule from 'app/shared/shared.module';

@Component({
  selector: 'jhi-account-list',
  templateUrl: './account-list.component.html',
  styleUrls: ['./account-list.component.scss'],
  imports: [SharedModule, RouterModule],
})
export class AccountListComponent {
  // implements OnInit
  theme = 'light';
  isLoading = false;

  accounts: FinancialAccount[] | null = null;
  bankSum = 0;
  investmentSum = 0;
  liabilitySum = 0;
  creditSum = 0;
  assetSum = 0;

  totalSum = 0;
  totalInvestmentsSum = 0;
  totalLiabilitiesSum = 0;
  totalCashSum = 0;
  totalSumWithoutAssets = 0;

  filterBanking = false;
  filterCredit = false;
  filterInvestment = false;
  filterLoans = false;
  filterAssets = false;

  expandedBanking = true;
  expandedCredit = true;
  expandedInvestment = true;
  expandedLoans = true;
  expandedAssets = true;

  title = '';

  constructor(
    private accountListService: AccountList,
    private themeService: ThemeService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();

    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
    });

    this.title = this.getPageTitle(this.router.routerState.snapshot.root);
  }

  get onlyBankAccounts(): FinancialAccount[] | undefined {
    return this.accounts?.filter(x => (x.accountType === 'bank' && x.relatedToAccountId == null) || x.accountType === 'cash');
  }

  get onlyCreditAccounts(): FinancialAccount[] | undefined {
    return this.accounts?.filter(x => x.accountType === 'credit');
  }

  get onlyLiabilityAccounts(): FinancialAccount[] | undefined {
    return this.accounts?.filter(x => x.accountType === 'liability' || x.accountType === 'loan');
  }

  get onlyAssetAccounts(): FinancialAccount[] | undefined {
    return this.accounts?.filter(x => x.accountType === 'asset');
  }

  get onlyInvestmentAccounts(): FinancialAccount[] | undefined {
    return this.accounts?.filter(x => x.accountType === 'investment');
  }

  load(): void {
    // eslint-disable-next-line no-console
    console.log('Loading Accounts');
    this.isLoading = true;
    this.accountListService.get().subscribe(
      response => {
        this.isLoading = false;
        // eslint-disable-next-line no-console
        console.log(`Response: ${JSON.stringify(response)}`);
        this.accounts = response;

        this.sumAccounts();
      },
      error => {
        this.isLoading = false;
      },
      () => {
        // eslint-disable-next-line no-console
        console.log(`Completed`);
        this.isLoading = false;
      },
    );
  }

  sumAccounts(): void {
    this.totalSum = 0;
    this.totalCashSum = 0;
    this.totalInvestmentsSum = 0;
    this.totalLiabilitiesSum = 0;
    this.totalSumWithoutAssets = 0;

    let sum = 0;
    this.onlyBankAccounts?.forEach(element => {
      sum += this.subtotal(element);
    });

    this.totalSum += sum;
    this.totalCashSum += sum;
    this.bankSum = sum;

    sum = 0;
    this.onlyCreditAccounts?.forEach(element => {
      sum += this.subtotal(element);
    });

    this.totalLiabilitiesSum += sum;
    this.totalSum += sum;
    this.creditSum = sum;

    sum = 0;
    this.onlyLiabilityAccounts?.forEach(element => {
      sum += this.subtotal(element);
    });

    this.totalLiabilitiesSum += sum;
    this.totalSum += sum;
    this.liabilitySum = sum;

    sum = 0;
    this.onlyInvestmentAccounts?.forEach(element => {
      sum += this.subtotal(element);
    });

    this.totalSum += sum;
    this.investmentSum = sum;
    this.totalSumWithoutAssets = this.totalSum;

    sum = 0;
    this.onlyAssetAccounts?.forEach(element => {
      sum += this.subtotal(element);
    });

    this.totalSum += sum;
    this.assetSum = sum;
  }

  subtotal(account: FinancialAccount): number {
    // TODO: Fix hard coding of local currency here
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    if (account.currencyCode !== 'AUD' && account.fxRateToLocal != null) {
      // Convert
      return account.balance * account.fxRateToLocal;
    }

    return account.balance;
  }

  private getPageTitle(routeSnapshot: ActivatedRouteSnapshot): string {
    let title: string = routeSnapshot.data['pageTitle'] ? routeSnapshot.data['pageTitle'] : 'App';
    if (routeSnapshot.firstChild) {
      title = this.getPageTitle(routeSnapshot.firstChild) || title;
    }
    // // console.log("Page Title (NAV): " + title);
    if (routeSnapshot.firstChild) {
      title = this.getPageTitle(routeSnapshot.firstChild) || title;
      // // console.log("Page Title (from FirstChild): " + title);
    }
    return title;
  }
}

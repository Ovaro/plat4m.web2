import { Component, computed, inject, signal } from '@angular/core';
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
  theme = signal('light');
  isLoading = signal(false);
  accounts = signal<FinancialAccount[] | null>(null);

  filterBanking = signal(false);
  filterCredit = signal(false);
  filterInvestment = signal(false);
  filterLoans = signal(false);
  filterAssets = signal(false);

  expandedBanking = signal(true);
  expandedCredit = signal(true);
  expandedInvestment = signal(true);
  expandedLoans = signal(true);
  expandedAssets = signal(true);

  title = signal('');

  private themeService = inject(ThemeService);

  constructor(
    private accountListService: AccountList,

    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();

    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme.set(event.theme);
    });

    this.title.set(this.getPageTitle(this.router.routerState.snapshot.root));
  }

  readonly onlyBankAccounts = computed(
    () => this.accounts()?.filter(x => (x.accountType === 'bank' && x.relatedToAccountId == null) || x.accountType === 'cash') ?? [],
  );

  readonly onlyCreditAccounts = computed(() => this.accounts()?.filter(x => x.accountType === 'credit') ?? []);

  readonly onlyLiabilityAccounts = computed(
    () => this.accounts()?.filter(x => x.accountType === 'liability' || x.accountType === 'loan') ?? [],
  );

  readonly onlyAssetAccounts = computed(() => this.accounts()?.filter(x => x.accountType === 'asset') ?? []);

  readonly onlyInvestmentAccounts = computed(() => this.accounts()?.filter(x => x.accountType === 'investment') ?? []);

  readonly bankSum = computed(() => this.sumGroup(this.onlyBankAccounts()));
  readonly creditSum = computed(() => this.sumGroup(this.onlyCreditAccounts()));
  readonly liabilitySum = computed(() => this.sumGroup(this.onlyLiabilityAccounts()));
  readonly investmentSum = computed(() => this.sumGroup(this.onlyInvestmentAccounts()));
  readonly assetSum = computed(() => this.sumGroup(this.onlyAssetAccounts()));

  readonly totalCashSum = computed(() => this.bankSum());
  readonly totalLiabilitiesSum = computed(() => this.creditSum() + this.liabilitySum());
  readonly totalInvestmentsSum = computed(() => this.investmentSum());
  readonly totalSumWithoutAssets = computed(() => this.bankSum() + this.creditSum() + this.liabilitySum() + this.investmentSum());
  readonly totalSum = computed(() => this.totalSumWithoutAssets() + this.assetSum());

  load(): void {
    // eslint-disable-next-line no-console
    console.log('Loading Accounts');
    this.isLoading.set(true);
    this.accountListService.get().subscribe({
      next: response => {
        this.isLoading.set(false);
        console.log(`Response: ${JSON.stringify(response)}`);
        this.accounts.set(response);
      },
      error: () => {
        this.isLoading.set(false);
      },
      complete: () => {
        console.log(`Completed`);
        this.isLoading.set(false);
      },
    });
  }

  private sumGroup(accounts: FinancialAccount[]): number {
    let sum = 0;
    accounts.forEach(element => {
      sum += this.subtotal(element);
    });
    return sum;
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

  toggleFilterBanking(): void {
    this.filterBanking.update(value => !value);
  }

  toggleFilterLoans(): void {
    this.filterLoans.update(value => !value);
  }

  toggleFilterCredit(): void {
    this.filterCredit.update(value => !value);
  }

  toggleFilterInvestment(): void {
    this.filterInvestment.update(value => !value);
  }

  toggleFilterAssets(): void {
    this.filterAssets.update(value => !value);
  }

  toggleExpandedBanking(): void {
    this.expandedBanking.update(value => !value);
  }

  toggleExpandedLoans(): void {
    this.expandedLoans.update(value => !value);
  }

  toggleExpandedCredit(): void {
    this.expandedCredit.update(value => !value);
  }

  toggleExpandedInvestment(): void {
    this.expandedInvestment.update(value => !value);
  }

  toggleExpandedAssets(): void {
    this.expandedAssets.update(value => !value);
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

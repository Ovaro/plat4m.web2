import { EventEmitter } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { ThemeService } from '../../layouts/main/theme.service';
import { CookieService } from 'ngx-cookie';
import { AccountList } from '../account-list/account-list.service';
import { InvestmentPortfolio } from '../investment-portfolio/investment-portfolio.service';
import { InvestmentComponent } from './investment.component';
import { InvestmentTransactions } from './investment.service';

describe('InvestmentComponent', () => {
  let component: InvestmentComponent;
  let fixture: ComponentFixture<InvestmentComponent>;

  const investmentPortfolioMock = {
    get: vitest.fn(() =>
      of([
        {
          id: 'security-1',
          name: 'BHP Group',
          symbol: 'BHP',
          userSymbol: 'BHP',
          currencyCode: 'AUD',
          type: 0,
          typeName: 'STOCK',
          sector: 'Materials',
          industry: 'Mining',
          exchangeName: 'ASX',
          linked: null,
          masterGuid: '',
          comment: '',
          quantity: 10,
          price: 25.4,
          value: 254,
          valueInBaseCurrency: 254,
          fxRateToLocal: null,
          fxDateTime: null,
          accountId: 'account-1',
          accountName: 'Brokerage',
          priceDateTime: '2026-06-18T00:00:00Z',
        },
      ]),
    ),
  };

  const investmentTransactionsMock = {
    getTransactions: vitest.fn(() => of([])),
    getSummary: vitest.fn(() =>
      of({
        priceDate: '2026-06-18',
        price: 25.4,
        quantity: 10,
        estimatedPrice: false,
        totalCapitalInvested: 200,
        totalCapitalGain: 54,
        totalCurrencyGain: 0,
        totalIncome: 0,
        totalReturn: 54,
        totalIncomeCAGR: 0,
        totalIncomePC: 0,
        totalReturnCAGR: 0,
        totalReturnPC: 0,
        totalCapitalGainCAGR: 0,
        totalCapitalGainPC: 0,
        totalCurrencyGainCAGR: 0,
        totalCurrencyGainPC: 0,
        totalFees: 0,
        currencyIsoCode: 'AUD',
      }),
    ),
    getHistory: vitest.fn(() => of([])),
    refreshQuotes: vitest.fn(() => of({ requestedCount: 1, refreshedCount: 1, skippedCount: 0, failedCount: 0, items: [] })),
    getStoredPrices: vitest.fn(() => of([{ id: 'price-1', symbol: 'BHP', date: '2026-06-18', price: 25.4, comment: 'Close' }])),
    createStoredPrice: vitest.fn(() => of({ id: 'price-2', symbol: 'BHP', date: '2026-06-19', price: 26.1, comment: null })),
    updateStoredPrice: vitest.fn(() => of({ id: 'price-1', symbol: 'BHP', date: '2026-06-18', price: 25.6, comment: 'Adjusted' })),
    deleteStoredPrice: vitest.fn(() => of(void 0)),
    getLots: vitest.fn(() =>
      of([
        {
          originalLot: {
            id: 'lot-1',
            sourceId: 101,
            lotKey: '2026-01-10 | 10 @ 25.4',
            originalLotId: 'lot-1',
            originalSourceId: 101,
            originalBuyDate: '2026-01-10',
            originalQuantity: 10,
            originalPrice: 25.4,
            quantity: 10,
            lotType: 0,
            accountId: 'account-1',
            accountName: 'Brokerage',
            securityId: 'security-1',
            securityName: 'BHP Group',
            buyDate: '2026-01-10',
            sellDate: null,
            openDate: '2026-01-10',
            closeDate: null,
            buyTransactionId: 'txn-1',
            sellTransactionId: null,
            openTransactionId: 'txn-1',
            closeTransactionId: null,
            costBasis: null,
            saleProceeds: null,
            realisedGainLoss: null,
          },
          remainingLot: {
            id: 'lot-1',
            sourceId: 101,
            lotKey: '2026-01-10 | 10 @ 25.4',
            originalLotId: 'lot-1',
            originalSourceId: 101,
            originalBuyDate: '2026-01-10',
            originalQuantity: 10,
            originalPrice: 25.4,
            quantity: 10,
            lotType: 0,
            accountId: 'account-1',
            accountName: 'Brokerage',
            securityId: 'security-1',
            securityName: 'BHP Group',
            buyDate: '2026-01-10',
            sellDate: null,
            openDate: '2026-01-10',
            closeDate: null,
            buyTransactionId: 'txn-1',
            sellTransactionId: null,
            openTransactionId: 'txn-1',
            closeTransactionId: null,
            costBasis: null,
            saleProceeds: null,
            realisedGainLoss: null,
          },
          lots: [
            {
              id: 'lot-1',
              sourceId: 101,
              lotKey: '2026-01-10 | 10 @ 25.4',
              originalLotId: 'lot-1',
              originalSourceId: 101,
              originalBuyDate: '2026-01-10',
              originalQuantity: 10,
              originalPrice: 25.4,
              quantity: 10,
              lotType: 0,
              accountId: 'account-1',
              accountName: 'Brokerage',
              securityId: 'security-1',
              securityName: 'BHP Group',
              buyDate: '2026-01-10',
              sellDate: null,
              openDate: '2026-01-10',
              closeDate: null,
              buyTransactionId: 'txn-1',
              sellTransactionId: null,
              openTransactionId: 'txn-1',
              closeTransactionId: null,
              costBasis: null,
              saleProceeds: null,
              realisedGainLoss: null,
            },
          ],
        },
      ]),
    ),
  };

  const themeServiceMock = {
    theme: () => 'light',
    onChange: new EventEmitter(),
  };

  const cookieServiceMock = {
    get: () => undefined,
    put: () => undefined,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvestmentComponent],
      providers: [
        { provide: InvestmentPortfolio, useValue: investmentPortfolioMock },
        { provide: InvestmentTransactions, useValue: investmentTransactionsMock },
        { provide: ThemeService, useValue: themeServiceMock },
        { provide: AccountList, useValue: {} },
        { provide: CookieService, useValue: cookieServiceMock },
        { provide: ActivatedRoute, useValue: { params: of({ id: 'security-1' }) } },
        {
          provide: Router,
          useValue: {
            navigate: () => Promise.resolve(true),
            routerState: { snapshot: { root: { data: { pageTitle: 'Investment' } } } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InvestmentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('refreshes quotes for the selected holding and reloads the page state', () => {
    const loadSpy = vitest.spyOn(component, 'load');

    component.refreshQuotes();

    expect(investmentTransactionsMock.refreshQuotes).toHaveBeenCalledWith('security-1');
    expect(loadSpy).toHaveBeenCalled();
    expect(component.quoteRefreshMessage).toContain('Updated 1 holding');
  });

  it('shows backend refresh errors to the user', () => {
    investmentTransactionsMock.refreshQuotes.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ error: { detail: 'No quote source available for this holding.' }, status: 503 })),
    );

    component.refreshQuotes();

    expect(component.quoteRefreshMessage).toBe('No quote source available for this holding.');
    expect(component.isRefreshingQuotes).toBe(false);
  });

  it('loads stored prices when opening the maintain prices dialog', () => {
    component.openPriceDialog();

    expect(investmentTransactionsMock.getStoredPrices).toHaveBeenCalledWith('security-1');
    expect(component.priceDialogVisible).toBe(true);
    expect(component.storedPrices).toHaveLength(1);
    expect(component.priceForm.controls.date.value).toBe(new Intl.DateTimeFormat('en-CA').format(new Date()));
    expect(component.priceForm.controls.price.value).toBeNull();
  });

  it('loads lots when opening the lots dialog', () => {
    component.openLotsDialog();

    expect(investmentTransactionsMock.getLots).toHaveBeenCalledWith('security-1');
    expect(component.lotsDialogVisible).toBe(true);
    expect(component.lotGroups).toHaveLength(1);
    expect(component.sortedLotRows).toHaveLength(1);
    expect(component.sortedLotFamilies[0].key).toBe('2026-01-10 | 10 @ 25.4');
    expect(component.openLotCount).toBe(1);
  });

  it('populates the editor when a historical price is selected', () => {
    const storedPrice = { id: 'price-1', symbol: 'BHP', date: '2026-06-18', price: 25.4, comment: 'Close' };

    component.selectStoredPrice(storedPrice);

    expect(component.editingStoredPrice).toEqual(storedPrice);
    expect(component.priceForm.getRawValue()).toEqual({ date: '2026-06-18', price: 25.4 });
  });

  it('starts a blank price dated today when Add is pressed from edit mode', () => {
    component.selectStoredPrice({ id: 'price-1', symbol: 'BHP', date: '2026-06-18', price: 25.4, comment: null });

    component.startAddingStoredPrice();

    expect(component.editingStoredPrice).toBeNull();
    expect(component.priceForm.controls.date.value).toBe(new Intl.DateTimeFormat('en-CA').format(new Date()));
    expect(component.priceForm.controls.price.value).toBeNull();
  });

  it('closes the actions menu before refreshing prices', () => {
    component.actionsMenuOpen = true;

    component.refreshQuotesFromActionsMenu();

    expect(component.actionsMenuOpen).toBe(false);
    expect(investmentTransactionsMock.refreshQuotes).toHaveBeenCalledWith('security-1');
  });
});

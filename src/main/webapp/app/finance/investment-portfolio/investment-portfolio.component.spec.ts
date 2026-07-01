import { EventEmitter } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { ThemeService } from '../../layouts/main/theme.service';
import { CookieService } from 'ngx-cookie';
import { AccountList } from '../account-list/account-list.service';
import { InvestmentPortfolioComponent } from './investment-portfolio.component';
import { InvestmentPortfolio } from './investment-portfolio.service';
import { AlertService } from 'app/core/util/alert.service';

describe('InvestmentPortfolioComponent', () => {
  let component: InvestmentPortfolioComponent;
  let fixture: ComponentFixture<InvestmentPortfolioComponent>;
  let activeQuoteRefreshResult: any = null;

  const investmentPortfolioMock = {
    get: vitest.fn(() => of([])),
    getSummaries: vitest.fn(() =>
      of({
        date: '2026-06-19',
        summaries: [],
        priceDate: '2026-06-18',
        price: 0,
        quantity: 0,
        estimatedPrice: false,
        totalCapitalInvested: 0,
        totalCapitalGain: 0,
        totalCurrencyGain: 0,
        totalIncome: 0,
        totalReturn: 0,
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
    getPortfolioHistory: vitest.fn(() => of([])),
    refreshQuotes: vitest.fn(() =>
      of({
        jobId: 'job-1',
        status: 'running',
        currentSymbol: 'BHP',
        currentMessage: 'Refreshing quotes.',
        complete: false,
        startedAtEpochMs: 1,
        completedAtEpochMs: null,
        requestedCount: 2,
        processedCount: 0,
        refreshedCount: 0,
        skippedCount: 0,
        failedCount: 0,
        appliedCount: 0,
        applyRequested: false,
        items: [],
      }),
    ),
    getRefreshQuoteStatus: vitest.fn(() =>
      of({
        jobId: 'job-1',
        status: 'completed',
        currentSymbol: null,
        currentMessage: 'Quote refresh completed.',
        complete: true,
        startedAtEpochMs: 1,
        completedAtEpochMs: 2,
        requestedCount: 2,
        processedCount: 2,
        refreshedCount: 2,
        skippedCount: 0,
        failedCount: 0,
        appliedCount: 2,
        applyRequested: true,
        items: [],
      }),
    ),
    updateRefreshQuoteSelection: vitest.fn(() =>
      of({
        jobId: 'job-1',
        status: 'running',
        currentSymbol: 'BHP',
        currentMessage: 'Refreshing quotes.',
        complete: false,
        startedAtEpochMs: 1,
        completedAtEpochMs: null,
        requestedCount: 2,
        processedCount: 0,
        refreshedCount: 0,
        skippedCount: 0,
        failedCount: 0,
        appliedCount: 0,
        applyRequested: false,
        items: [],
      }),
    ),
    applyRefreshQuotes: vitest.fn(() =>
      of({
        jobId: 'job-1',
        status: 'running',
        currentSymbol: 'BHP',
        currentMessage: 'Selected quotes applied.',
        complete: false,
        startedAtEpochMs: 1,
        completedAtEpochMs: null,
        requestedCount: 2,
        processedCount: 0,
        refreshedCount: 0,
        skippedCount: 0,
        failedCount: 0,
        appliedCount: 1,
        applyRequested: true,
        items: [],
      }),
    ),
    getActiveQuoteRefreshResult: vitest.fn(() => activeQuoteRefreshResult),
    setActiveQuoteRefreshResult: vitest.fn(result => {
      activeQuoteRefreshResult = result;
    }),
    clearActiveQuoteRefreshResult: vitest.fn(() => {
      activeQuoteRefreshResult = null;
    }),
  };

  const accountListMock = {
    getByType: vitest.fn(() => of([])),
  };

  const themeServiceMock = {
    theme: () => 'light',
    onChange: new EventEmitter(),
  };

  const cookieServiceMock = {
    get: () => undefined,
    put: () => undefined,
  };

  const alertServiceMock = {
    addAlert: vitest.fn(),
  };

  beforeEach(async () => {
    activeQuoteRefreshResult = null;
    alertServiceMock.addAlert.mockReset();
    investmentPortfolioMock.refreshQuotes.mockClear();
    investmentPortfolioMock.getRefreshQuoteStatus.mockClear();
    investmentPortfolioMock.getActiveQuoteRefreshResult.mockClear();
    investmentPortfolioMock.setActiveQuoteRefreshResult.mockClear();
    investmentPortfolioMock.clearActiveQuoteRefreshResult.mockClear();
    investmentPortfolioMock.updateRefreshQuoteSelection.mockClear();
    investmentPortfolioMock.applyRefreshQuotes.mockClear();
    await TestBed.configureTestingModule({
      imports: [InvestmentPortfolioComponent],
      providers: [
        { provide: InvestmentPortfolio, useValue: investmentPortfolioMock },
        { provide: ThemeService, useValue: themeServiceMock },
        { provide: AccountList, useValue: accountListMock },
        { provide: CookieService, useValue: cookieServiceMock },
        { provide: AlertService, useValue: alertServiceMock },
        { provide: ActivatedRoute, useValue: { params: of({}) } },
        {
          provide: Router,
          useValue: {
            navigate: () => Promise.resolve(true),
            routerState: { snapshot: { root: { data: { pageTitle: 'Portfolio' } } } },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InvestmentPortfolioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('refreshes quotes for the current account scope and reloads portfolio data', () => {
    component.accountId = 'account-1';
    vitest.useFakeTimers();
    const loadSpy = vitest.spyOn(component, 'load');

    component.refreshQuotes();
    vitest.advanceTimersByTime(2000);

    expect(investmentPortfolioMock.refreshQuotes).toHaveBeenCalledWith('account-1');
    expect(investmentPortfolioMock.getRefreshQuoteStatus).toHaveBeenCalledWith('job-1');
    expect(loadSpy).toHaveBeenCalled();
    expect(component.quoteRefreshMessage).toBeNull();
    expect(component.quoteRefreshDialogVisible).toBe(true);
    expect(component.quoteRefreshResult?.status).toBe('completed');
    expect(alertServiceMock.addAlert).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'success',
        toast: true,
      }),
    );
    vitest.useRealTimers();
  });

  it('keeps the spinner active while the refresh job is running and reopens the dialog on button click', () => {
    investmentPortfolioMock.refreshQuotes.mockReturnValueOnce(
      of({
        jobId: 'job-live',
        status: 'running',
        currentSymbol: 'BHP',
        currentMessage: 'Refreshing quotes.',
        complete: false,
        startedAtEpochMs: 1,
        completedAtEpochMs: null,
        requestedCount: 2,
        processedCount: 0,
        refreshedCount: 0,
        skippedCount: 0,
        failedCount: 0,
        appliedCount: 0,
        applyRequested: false,
        items: [
          {
            userSecurityId: 'holding-1',
            symbol: 'BHP',
            requestedSymbol: 'BHP',
            requestedExchangeMic: 'XASX',
            currencyCode: 'AUD',
            selected: true,
            applied: false,
            canApply: true,
            appliedSource: null,
            status: 'pending',
            message: 'Waiting on Twelve Data confirmation.',
            priceDate: null,
            price: null,
            aiPriceDate: '2026-06-19',
            aiPrice: 49.85,
            aiMessage: 'AI batch quote loaded.',
            twelveDataPriceDate: null,
            twelveDataPrice: null,
            twelveDataMessage: null,
            previousPrice: 50,
            priceDeltaValue: null,
            priceDeltaPercent: null,
          },
        ],
      }),
    );
    investmentPortfolioMock.getRefreshQuoteStatus.mockReturnValueOnce(
      of({
        jobId: 'job-live',
        status: 'running',
        currentSymbol: 'CBA',
        currentMessage: 'Processed 1 of 2 holdings.',
        complete: false,
        startedAtEpochMs: 1,
        completedAtEpochMs: null,
        requestedCount: 2,
        processedCount: 1,
        refreshedCount: 1,
        skippedCount: 0,
        failedCount: 0,
        appliedCount: 0,
        applyRequested: false,
        items: [],
      }),
    );

    component.refreshQuotes();

    expect(component.isRefreshingQuotes).toBe(true);
    expect(component.quoteRefreshDialogVisible).toBe(true);
    expect(component.quoteRefreshItems[0].aiPrice).toBe(49.85);
    expect(component.quoteRefreshItems[0].status).toBe('pending');

    component.quoteRefreshDialogVisible = false;
    component.refreshQuotes();

    expect(component.quoteRefreshDialogVisible).toBe(true);
    expect(investmentPortfolioMock.refreshQuotes).toHaveBeenCalledTimes(1);
    expect(investmentPortfolioMock.getRefreshQuoteStatus).toHaveBeenCalledWith('job-live');
    expect(component.quoteRefreshResult?.currentSymbol).toBe('CBA');
    expect(component.isRefreshingQuotes).toBe(true);
  });

  it('resumes an active refresh after navigating back to the portfolio page', () => {
    activeQuoteRefreshResult = {
      jobId: 'job-resume',
      status: 'running',
      currentSymbol: 'WOW',
      currentMessage: 'Refreshing WOW.',
      complete: false,
      startedAtEpochMs: 1,
      completedAtEpochMs: null,
      requestedCount: 3,
      processedCount: 1,
      refreshedCount: 0,
      skippedCount: 0,
      failedCount: 0,
      appliedCount: 0,
      applyRequested: false,
      items: [
        {
          userSecurityId: 'holding-1',
          symbol: 'WOW',
          requestedSymbol: 'WOW',
          requestedExchangeMic: 'XASX',
          currencyCode: 'AUD',
          selected: true,
          applied: false,
          canApply: true,
          appliedSource: null,
          status: 'pending',
          message: 'Waiting on Twelve Data confirmation.',
          priceDate: null,
          price: null,
          aiPriceDate: '2026-06-19',
          aiPrice: 31.45,
          aiMessage: 'AI batch quote loaded.',
          twelveDataPriceDate: null,
          twelveDataPrice: null,
          twelveDataMessage: null,
          previousPrice: 30.9,
          priceDeltaValue: null,
          priceDeltaPercent: null,
        },
      ],
    };
    investmentPortfolioMock.getRefreshQuoteStatus.mockReturnValueOnce(
      of({
        ...activeQuoteRefreshResult,
        currentSymbol: 'BHP',
        currentMessage: 'Processed 2 of 3 holdings.',
        processedCount: 2,
      }),
    );

    fixture = TestBed.createComponent(InvestmentPortfolioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(investmentPortfolioMock.getRefreshQuoteStatus).toHaveBeenCalledWith('job-resume');
    expect(component.isRefreshingQuotes).toBe(true);
    expect(component.quoteRefreshResult?.jobId).toBe('job-resume');
    expect(component.quoteRefreshResult?.currentSymbol).toBe('BHP');

    component.refreshQuotes();

    expect(investmentPortfolioMock.refreshQuotes).not.toHaveBeenCalled();
    expect(component.quoteRefreshDialogVisible).toBe(true);
  });

  it('shows backend refresh errors to the user', () => {
    investmentPortfolioMock.refreshQuotes.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ error: { detail: 'Market data refresh is disabled.' }, status: 503 })),
    );

    component.refreshQuotes();

    expect(component.quoteRefreshMessage).toBe('Market data refresh is disabled.');
    expect(component.isRefreshingQuotes).toBe(false);
  });

  it('shows refresh details in a dialog when some refreshes are skipped or fail', () => {
    investmentPortfolioMock.refreshQuotes.mockReturnValueOnce(
      of({
        jobId: 'job-2',
        status: 'completed',
        currentSymbol: null,
        currentMessage: 'Quote refresh completed.',
        complete: true,
        startedAtEpochMs: 1,
        completedAtEpochMs: 2,
        requestedCount: 21,
        processedCount: 21,
        refreshedCount: 14,
        skippedCount: 5,
        failedCount: 2,
        appliedCount: 0,
        applyRequested: false,
        items: [
          {
            requestedSymbol: 'XYZ',
            requestedExchangeMic: 'XASX',
            selected: true,
            applied: false,
            canApply: false,
            appliedSource: null,
            status: 'skipped',
            aiPriceDate: null,
            aiPrice: null,
            aiMessage: 'No AI quote returned.',
            twelveDataPriceDate: null,
            twelveDataPrice: null,
            twelveDataMessage: null,
            previousPrice: 10,
            price: null,
            priceDeltaValue: null,
            priceDeltaPercent: null,
            message:
              'Requested code XYZ skipped. Only AUD and USD holdings with a linked security symbol can be refreshed. Details: currency=EUR.',
          },
          {
            requestedSymbol: 'BHP',
            requestedExchangeMic: 'XASX',
            selected: true,
            applied: false,
            canApply: true,
            appliedSource: null,
            status: 'failed',
            aiPriceDate: '2026-06-19',
            aiPrice: 49.85,
            aiMessage: 'AI provisional quote refreshed.',
            twelveDataPriceDate: null,
            twelveDataPrice: null,
            twelveDataMessage: 'Twelve Data EOD request failed with HTTP 404.',
            previousPrice: 50,
            price: null,
            priceDeltaValue: null,
            priceDeltaPercent: null,
            message: 'Requested code BHP failed: Twelve Data EOD request failed with HTTP 404.',
          },
        ],
      }),
    );

    component.refreshQuotes();

    expect(component.quoteRefreshMessage).toBeNull();
    expect(component.quoteRefreshDialogVisible).toBe(true);
    expect(component.quoteRefreshDialogTitle).toContain('Processed 21 holdings: 14 refreshed, 5 skipped, 2 failed');
    expect(component.quoteRefreshItems[0].requestedSymbol).toBe('XYZ');
    expect(component.quoteRefreshItems[1].aiPrice).toBe(49.85);
    expect(component.quoteRefreshItems[1].twelveDataMessage).toContain('HTTP 404');
    expect(component.quoteRefreshItems[1].message).toContain('HTTP 404');
  });
});

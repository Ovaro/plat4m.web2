import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { finalize, timeout } from 'rxjs';
import SharedModule from 'app/shared/shared.module';

import { DemoDataService } from './demo-data.service';
import { DemoDataAccountSpec, DemoDataBatch, DemoDataOptions, DemoDataRequest, DemoDataResult } from './demo-data.types';

@Component({
  selector: 'jhi-demo-data',
  templateUrl: './demo-data.component.html',
  styleUrl: './demo-data.component.scss',
  imports: [SharedModule],
})
export class DemoDataComponent implements OnInit {
  options: DemoDataOptions = {
    personas: ['Single', 'Couple', 'Family', 'Retiree'],
    variants: [],
    accountTypes: ['bank', 'credit', 'loan', 'asset', 'investment'],
    yearOptions: [1, 2, 3, 5, 7, 10],
  };
  request: DemoDataRequest = this.defaultRequest();
  previewResult: DemoDataResult | null = null;
  generateResult: DemoDataResult | null = null;
  batches: DemoDataBatch[] = [];
  loadingPreview = false;
  generating = false;
  loadingBatches = false;
  deletingBatchId: string | null = null;
  errorMessage = '';
  statusMessage = '';
  successMessage = '';

  private readonly demoDataService = inject(DemoDataService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  get currentResult(): DemoDataResult | null {
    return this.previewResult ?? this.generateResult;
  }

  get hasBatches(): boolean {
    return this.batches.length > 0;
  }

  ngOnInit(): void {
    this.demoDataService.options().subscribe({
      next: options => (this.options = options),
      error: error => (this.errorMessage = this.errorText(error, 'Unable to load demo data options.')),
    });
    this.loadBatches();
  }

  preview(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.statusMessage = 'Preparing a preview. This usually only takes a few seconds.';
    this.loadingPreview = true;
    this.previewResult = null;
    this.demoDataService
      .preview(this.request)
      .pipe(
        timeout({ first: 45000 }),
        finalize(() => {
          this.loadingPreview = false;
        }),
      )
      .subscribe({
        next: result => {
          this.previewResult = result;
          this.statusMessage = '';
          this.successMessage = `Preview ready: ${result.accountCount} accounts and ${result.transactionCount} transactions would be created.`;
        },
        error: error => {
          this.statusMessage = '';
          this.errorMessage = this.errorText(error, 'Unable to preview demo data.');
        },
      });
  }

  generate(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.statusMessage = 'Creating generated accounts, transactions, payees, and holdings. Large histories can take a little longer.';
    this.generating = true;
    this.generateResult = null;
    this.demoDataService
      .generate(this.request)
      .pipe(
        timeout({ first: 90000 }),
        finalize(() => {
          this.generating = false;
        }),
      )
      .subscribe({
        next: result => {
          this.generateResult = result;
          this.previewResult = null;
          this.statusMessage = '';
          this.successMessage = `Created ${result.accountCount} accounts and ${result.transactionCount} transactions as generated demo data.`;
          this.loadBatches();
        },
        error: error => {
          this.statusMessage = '';
          this.errorMessage = this.errorText(error, 'Unable to generate demo data.');
        },
      });
  }

  loadBatches(): void {
    this.errorMessage = '';
    this.loadingBatches = true;
    this.demoDataService
      .batches()
      .pipe(timeout({ first: 45000 }))
      .subscribe({
        next: batches => {
          this.batches = this.normalizeBatches(batches);
          this.loadingBatches = false;
          this.changeDetectorRef.detectChanges();
        },
        error: error => {
          this.batches = [];
          this.loadingBatches = false;
          this.errorMessage = this.errorText(error, 'Unable to load generated demo batches.');
          this.changeDetectorRef.detectChanges();
        },
      });
  }

  deleteBatch(batch: DemoDataBatch): void {
    if (!globalThis.confirm(`Delete generated batch "${batch.generatedLabel}" and its generated accounts, transactions, and holdings?`)) {
      return;
    }
    this.errorMessage = '';
    this.successMessage = '';
    this.statusMessage = `Deleting generated batch ${batch.generatedLabel}.`;
    this.deletingBatchId = batch.generatedBatchId;
    this.demoDataService
      .deleteBatch(batch.generatedBatchId)
      .pipe(
        timeout({ first: 90000 }),
        finalize(() => {
          this.deletingBatchId = null;
        }),
      )
      .subscribe({
        next: result => {
          this.statusMessage = '';
          this.successMessage = `Deleted ${result.accountCount} accounts and ${result.transactionCount} transactions from ${result.generatedLabel}.`;
          this.generateResult = null;
          this.previewResult = null;
          this.loadBatches();
        },
        error: error => {
          this.statusMessage = '';
          this.errorMessage = this.errorText(error, 'Unable to delete generated batch.');
        },
      });
  }

  applyPersonaDefaults(): void {
    const family = this.request.persona === 'Family';
    const retiree = this.request.persona === 'Retiree';
    this.request.privateSchool = family && this.request.privateSchool;
    this.request.homeOwner = family || retiree || this.request.homeOwner;
    this.request.renter = !this.request.homeOwner && this.request.renter;
    this.account('loan').include = this.request.homeOwner || this.request.realEstateInvestor;
    this.account('asset').include = family || this.request.homeOwner || this.request.realEstateInvestor;
    this.account('investment').include = !this.request.noInvestments;
  }

  variantChanged(): void {
    if (this.request.noInvestments) {
      this.request.domesticStocks = false;
      this.request.internationalStocks = false;
      this.account('investment').include = false;
    } else {
      this.account('investment').include = true;
      this.request.domesticStocks = true;
    }

    if (this.request.homeOwner || this.request.realEstateInvestor) {
      this.request.renter = false;
      this.account('loan').include = true;
      this.account('asset').include = true;
    }
  }

  account(type: DemoDataAccountSpec['accountType']): DemoDataAccountSpec {
    return this.request.accounts.find(account => account.accountType === type)!;
  }

  private defaultRequest(): DemoDataRequest {
    return {
      persona: 'Family',
      currencyCode: 'AUD',
      hints: '',
      domesticStocks: true,
      internationalStocks: false,
      realEstateInvestor: false,
      homeOwner: true,
      renter: false,
      privateSchool: false,
      noInvestments: false,
      useAiHints: false,
      spendingRatePercent: 55,
      accounts: [
        { accountType: 'bank', include: true, accountCount: 1, years: 3, density: 'high', startingValue: 3500, hints: '' },
        { accountType: 'credit', include: true, accountCount: 1, years: 3, density: 'medium', startingValue: 0, hints: '' },
        { accountType: 'loan', include: true, accountCount: 1, years: 5, density: 'medium', startingValue: -620000, hints: '' },
        { accountType: 'asset', include: true, accountCount: 1, years: 1, density: 'low', startingValue: 950000, hints: '' },
        {
          accountType: 'investment',
          include: true,
          accountCount: 1,
          years: 5,
          density: 'medium',
          startingValue: 55000,
          stockCount: 4,
          tradingProfile: 'buy_hold',
          hints: '',
        },
      ],
    };
  }

  private errorText(error: unknown, fallback: string): string {
    const maybeError = error as { name?: string; error?: { detail?: string; message?: string } };
    if (maybeError.name === 'TimeoutError') {
      return `${fallback} The request timed out before the server responded.`;
    }
    return maybeError.error?.detail ?? maybeError.error?.message ?? fallback;
  }

  private normalizeBatches(response: unknown): DemoDataBatch[] {
    const value = this.responseBody(response);
    if (!Array.isArray(value)) {
      return [];
    }
    return value
      .filter((batch): batch is Record<string, unknown> => batch !== null && typeof batch === 'object')
      .map((batch, index) => ({
        generatedBatchId: this.stringValue(batch['generatedBatchId'], `batch-${index}`),
        generatedLabel: this.stringValue(batch['generatedLabel'], 'Generated demo batch'),
        createdAt: this.stringValue(batch['createdAt'], ''),
        accountCount: this.numberValue(batch['accountCount']),
        transactionCount: this.numberValue(batch['transactionCount']),
        payeeCount: this.numberValue(batch['payeeCount']),
        securityCount: this.numberValue(batch['securityCount']),
        priceCount: this.numberValue(batch['priceCount']),
        lotCount: this.numberValue(batch['lotCount']),
        accountNames: Array.isArray(batch['accountNames']) ? batch['accountNames'].map(name => this.stringValue(name, 'Account')) : [],
      }));
  }

  private responseBody(response: unknown): unknown {
    if (response !== null && typeof response === 'object' && 'body' in response) {
      return (response as { body?: unknown }).body;
    }
    if (response !== null && typeof response === 'object' && 'content' in response) {
      return (response as { content?: unknown }).content;
    }
    if (response !== null && typeof response === 'object' && 'data' in response) {
      return (response as { data?: unknown }).data;
    }
    return response;
  }

  private stringValue(value: unknown, fallback: string): string {
    return typeof value === 'string' && value.length > 0 ? value : fallback;
  }

  private numberValue(value: unknown): number {
    return typeof value === 'number' && Number.isFinite(value) ? value : 0;
  }
}

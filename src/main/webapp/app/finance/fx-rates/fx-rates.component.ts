import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexGrid,
  ApexMarkers,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  NgApexchartsModule,
} from 'ng-apexcharts';

import SharedModule from 'app/shared/shared.module';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';
import { FinanceFXHistoryPoint, FinanceFXImportRequest, FinanceFXRate, FinanceFXRateUpdate } from './fx-rates.types';
import { FxRatesService } from './fx-rates.service';

type FxHistoryChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  stroke: ApexStroke;
  dataLabels: ApexDataLabels;
  markers: ApexMarkers;
  tooltip: ApexTooltip;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  grid: ApexGrid;
  colors: string[];
};

@Component({
  selector: 'jhi-fx-rates',
  templateUrl: './fx-rates.component.html',
  styleUrls: ['./fx-rates.component.scss'],
  imports: [SharedModule, ReactiveFormsModule, DialogModule, NgApexchartsModule],
})
export class FxRatesComponent implements OnInit {
  protected rates: FinanceFXRate[] = [];
  protected searchText = '';
  protected isLoading = false;
  protected isSaving = false;
  protected isImporting = false;
  protected editorVisible = false;
  protected importDialogVisible = false;
  protected historyDialogVisible = false;
  protected isHistoryLoading = false;
  protected errorMessage: string | null = null;
  protected historyErrorMessage: string | null = null;
  protected importMessage: string | null = null;
  protected editingRate: FinanceFXRate | null = null;
  protected deleteCandidate: FinanceFXRate | null = null;
  protected selectedHistoryRate: FinanceFXRate | null = null;
  protected historyRates: FinanceFXRate[] = [];
  protected historyInverted = false;
  protected theme = 'light';
  protected historyChartOptions: Partial<FxHistoryChartOptions> = {};
  protected editorPairLocked = false;
  protected editorContext: 'default' | 'history' = 'default';

  protected readonly form = new FormGroup({
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    fromIsoCode: new FormControl('AUD', { nonNullable: true, validators: [Validators.required, Validators.minLength(3)] }),
    toIsoCode: new FormControl('USD', { nonNullable: true, validators: [Validators.required, Validators.minLength(3)] }),
    rate: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(0.0000001)] }),
  });

  protected readonly importForm = new FormGroup({
    baseCurrency: new FormControl('AUD', { nonNullable: true, validators: [Validators.required, Validators.minLength(3)] }),
    quoteCurrencies: new FormControl('USD, EUR, GBP, NZD, CAD, JPY', { nonNullable: true, validators: [Validators.required] }),
    date: new FormControl<string | null>(null),
  });

  private readonly fxRatesService = inject(FxRatesService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly themeService = inject(ThemeService);

  ngOnInit(): void {
    this.theme = this.themeService.theme();
    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
      this.syncHistoryChart();
      this.changeDetectorRef.markForCheck();
    });

    this.load();
    this.syncHistoryChart();
  }

  get filteredRates(): FinanceFXRate[] {
    const query = this.searchText.trim().toLowerCase();
    return this.rates.filter(rate => {
      const date = this.getDateInputValue(rate.date);
      const pair = `${rate.fromIsoCode}/${rate.toIsoCode}`.toLowerCase();
      return !query || date.includes(query) || pair.includes(query) || String(rate.rate).includes(query);
    });
  }

  get favouriteRates(): FinanceFXRate[] {
    return this.filteredRates.filter(rate => rate.favourite);
  }

  get otherRates(): FinanceFXRate[] {
    return this.filteredRates.filter(rate => !rate.favourite);
  }

  get deleteDialogVisible(): boolean {
    return this.deleteCandidate !== null;
  }

  set deleteDialogVisible(visible: boolean) {
    if (!visible) {
      this.deleteCandidate = null;
    }
  }

  get historyDialogTitle(): string {
    if (!this.selectedHistoryRate) {
      return 'FX history';
    }

    return `${this.historyFromIsoCode} / ${this.historyToIsoCode}`;
  }

  get historyFromIsoCode(): string {
    if (!this.selectedHistoryRate) {
      return '';
    }

    return this.historyInverted ? this.selectedHistoryRate.toIsoCode : this.selectedHistoryRate.fromIsoCode;
  }

  get historyToIsoCode(): string {
    if (!this.selectedHistoryRate) {
      return '';
    }

    return this.historyInverted ? this.selectedHistoryRate.fromIsoCode : this.selectedHistoryRate.toIsoCode;
  }

  get visibleHistoryRates(): FinanceFXHistoryPoint[] {
    return this.historyRates.map(rate => ({
      date: rate.date,
      rate: this.transformRate(rate.rate),
    }));
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.fxRatesService.getRates().subscribe({
      next: rates => {
        this.rates = rates;
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Loading FX rates failed.');
        this.isLoading = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  openAddDialog(): void {
    this.editorContext = 'default';
    this.editorPairLocked = false;
    this.form.controls.fromIsoCode.enable({ emitEvent: false });
    this.form.controls.toIsoCode.enable({ emitEvent: false });
    this.editingRate = null;
    this.errorMessage = null;
    this.form.reset({
      date: new Date().toISOString().slice(0, 10),
      fromIsoCode: 'AUD',
      toIsoCode: 'USD',
      rate: null,
    });
    this.editorVisible = true;
  }

  openImportDialog(): void {
    this.importDialogVisible = true;
    this.errorMessage = null;
    this.importMessage = null;
  }

  openEditDialog(rate: FinanceFXRate): void {
    this.editorContext = 'default';
    this.editorPairLocked = false;
    this.form.controls.fromIsoCode.enable({ emitEvent: false });
    this.form.controls.toIsoCode.enable({ emitEvent: false });
    this.editingRate = rate;
    this.errorMessage = null;
    this.form.reset({
      date: this.getDateInputValue(rate.date),
      fromIsoCode: rate.fromIsoCode,
      toIsoCode: rate.toIsoCode,
      rate: rate.rate,
    });
    this.editorVisible = true;
  }

  openHistoryAddDialog(): void {
    if (!this.selectedHistoryRate) {
      return;
    }

    const today = new Date().toISOString().slice(0, 10);
    this.prepareHistoryEditor(
      null,
      today,
      this.historyInverted ? this.selectedHistoryRate.toIsoCode : this.selectedHistoryRate.fromIsoCode,
      this.historyInverted ? this.selectedHistoryRate.fromIsoCode : this.selectedHistoryRate.toIsoCode,
      null,
    );
  }

  openHistoryEditDialog(rate: FinanceFXRate): void {
    const displayedRate = this.historyInverted ? this.transformRate(rate.rate) : rate.rate;
    this.prepareHistoryEditor(
      rate,
      this.getDateInputValue(rate.date),
      this.historyInverted ? rate.toIsoCode : rate.fromIsoCode,
      this.historyInverted ? rate.fromIsoCode : rate.toIsoCode,
      displayedRate,
    );
  }

  openHistoryDialog(rate: FinanceFXRate): void {
    this.selectedHistoryRate = rate;
    this.historyDialogVisible = true;
    this.historyInverted = false;
    this.isHistoryLoading = true;
    this.historyErrorMessage = null;
    this.historyRates = [];
    this.syncHistoryChart();

    this.fxRatesService.getRateHistory(rate.fromIsoCode, rate.toIsoCode).subscribe({
      next: history => {
        this.historyRates = [...history].sort((left, right) => right.date.localeCompare(left.date));
        this.isHistoryLoading = false;
        this.syncHistoryChart();
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.historyErrorMessage = this.getErrorMessage(error, 'Loading FX history failed.');
        this.isHistoryLoading = false;
        this.syncHistoryChart();
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  closeHistoryDialog(): void {
    this.historyDialogVisible = false;
    this.selectedHistoryRate = null;
    this.historyRates = [];
    this.historyErrorMessage = null;
    this.isHistoryLoading = false;
    this.historyInverted = false;
    this.syncHistoryChart();
  }

  toggleHistoryDirection(): void {
    this.historyInverted = !this.historyInverted;
    this.syncHistoryChart();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawFromIsoCode = this.form.controls.fromIsoCode.getRawValue().trim().toUpperCase();
    const rawToIsoCode = this.form.controls.toIsoCode.getRawValue().trim().toUpperCase();
    const rawRate = this.form.controls.rate.value!;
    const update: FinanceFXRateUpdate =
      this.editorContext === 'history' && this.historyInverted
        ? {
            date: this.form.controls.date.value,
            fromIsoCode: rawToIsoCode,
            toIsoCode: rawFromIsoCode,
            rate: rawRate === 0 ? 0 : 1 / rawRate,
          }
        : {
            date: this.form.controls.date.value,
            fromIsoCode: rawFromIsoCode,
            toIsoCode: rawToIsoCode,
            rate: rawRate,
          };

    this.isSaving = true;
    this.errorMessage = null;
    const request = this.editingRate ? this.fxRatesService.updateRate(this.editingRate.id, update) : this.fxRatesService.createRate(update);
    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.editorVisible = false;
        this.resetEditorState();
        this.changeDetectorRef.markForCheck();
        this.refreshAfterRateMutation();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Saving the FX rate failed.');
        this.isSaving = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  confirmDelete(rate: FinanceFXRate): void {
    this.deleteCandidate = rate;
    this.errorMessage = null;
  }

  deleteRate(): void {
    if (!this.deleteCandidate) {
      return;
    }

    this.isSaving = true;
    this.fxRatesService.deleteRate(this.deleteCandidate.id).subscribe({
      next: () => {
        this.isSaving = false;
        this.deleteCandidate = null;
        this.changeDetectorRef.markForCheck();
        this.refreshAfterRateMutation();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Deleting the FX rate failed.');
        this.isSaving = false;
        this.deleteCandidate = null;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  toggleFavourite(rate: FinanceFXRate): void {
    this.fxRatesService.updateFavourite(rate.id, !rate.favourite).subscribe({
      next: updatedRate => {
        this.rates = this.rates.map(existing => (existing.id === rate.id ? { ...existing, favourite: updatedRate.favourite } : existing));
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Updating FX favourite failed.');
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  favouriteTitle(rate: FinanceFXRate): string {
    return rate.favourite ? 'Remove from favourites' : 'Add to favourites';
  }

  updateFromFrankfurter(): void {
    if (this.importForm.invalid) {
      this.importForm.markAllAsTouched();
      return;
    }

    const request: FinanceFXImportRequest = {
      baseCurrency: this.importForm.controls.baseCurrency.value.trim().toUpperCase(),
      quoteCurrencies: this.importForm.controls.quoteCurrencies.value
        .split(',')
        .map(currency => currency.trim().toUpperCase())
        .filter(Boolean),
      date: this.importForm.controls.date.value || null,
    };

    this.isImporting = true;
    this.errorMessage = null;
    this.importMessage = null;
    this.fxRatesService.importFrankfurterRates(request).subscribe({
      next: result => {
        this.importMessage = `Updated ${result.updated} ${result.baseCurrency} rate${result.updated === 1 ? '' : 's'} for ${result.date}.`;
        this.isImporting = false;
        this.importDialogVisible = false;
        this.changeDetectorRef.markForCheck();
        this.load();
      },
      error: error => {
        this.errorMessage = this.getErrorMessage(error, 'Updating FX rates failed.');
        this.isImporting = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  getDateInputValue(value: string): string {
    return value.slice(0, 10);
  }

  formatRate(rate: number): string {
    return new Intl.NumberFormat(undefined, {
      minimumFractionDigits: rate >= 1 ? 2 : 4,
      maximumFractionDigits: rate >= 1 ? 6 : 8,
    }).format(rate);
  }

  protected transformRate(rate: number): number {
    if (!this.historyInverted) {
      return rate;
    }

    return rate === 0 ? 0 : 1 / rate;
  }

  private prepareHistoryEditor(
    rate: FinanceFXRate | null,
    date: string,
    fromIsoCode: string,
    toIsoCode: string,
    value: number | null,
  ): void {
    this.editorContext = 'history';
    this.editorPairLocked = true;
    this.editingRate = rate;
    this.errorMessage = null;
    this.form.reset({
      date,
      fromIsoCode,
      toIsoCode,
      rate: value,
    });
    this.form.controls.fromIsoCode.disable({ emitEvent: false });
    this.form.controls.toIsoCode.disable({ emitEvent: false });
    this.editorVisible = true;
  }

  private refreshAfterRateMutation(): void {
    this.load();
    if (this.selectedHistoryRate) {
      this.reloadHistory();
    }
  }

  private reloadHistory(): void {
    if (!this.selectedHistoryRate) {
      return;
    }

    this.isHistoryLoading = true;
    this.historyErrorMessage = null;
    this.fxRatesService.getRateHistory(this.selectedHistoryRate.fromIsoCode, this.selectedHistoryRate.toIsoCode).subscribe({
      next: history => {
        this.historyRates = [...history].sort((left, right) => right.date.localeCompare(left.date));
        this.isHistoryLoading = false;
        const latestRate = this.historyRates[0] ?? null;
        if (latestRate) {
          this.selectedHistoryRate = latestRate;
        }
        this.syncHistoryChart();
        this.changeDetectorRef.markForCheck();
      },
      error: error => {
        this.historyErrorMessage = this.getErrorMessage(error, 'Loading FX history failed.');
        this.isHistoryLoading = false;
        this.syncHistoryChart();
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  protected resetEditorState(): void {
    this.editorContext = 'default';
    this.editorPairLocked = false;
    this.form.controls.fromIsoCode.enable({ emitEvent: false });
    this.form.controls.toIsoCode.enable({ emitEvent: false });
  }

  private syncHistoryChart(): void {
    const historySeries = [...this.visibleHistoryRates].reverse().map(rate => ({ x: this.getDateInputValue(rate.date), y: rate.rate }));
    const isDark = this.theme === 'dark';

    this.historyChartOptions = {
      series: [
        {
          name: this.historyDialogTitle,
          data: historySeries,
        },
      ],
      chart: {
        type: 'line',
        height: 220,
        toolbar: {
          show: false,
        },
        zoom: {
          enabled: false,
        },
        animations: {
          enabled: false,
        },
        sparkline: {
          enabled: false,
        },
      },
      colors: [isDark ? '#7dd3fc' : '#0f766e'],
      stroke: {
        width: 2,
        curve: 'smooth',
      },
      dataLabels: {
        enabled: false,
      },
      markers: {
        size: 0,
        hover: {
          size: 4,
        },
      },
      tooltip: {
        theme: isDark ? 'dark' : 'light',
        x: {
          format: 'yyyy-MM-dd',
        },
        y: {
          formatter: (value: number) => this.formatRate(value),
        },
      },
      xaxis: {
        type: 'datetime',
        labels: {
          style: {
            colors: isDark ? '#98a2b3' : '#667085',
          },
        },
        axisBorder: {
          color: isDark ? 'rgba(248, 250, 252, 0.12)' : 'rgba(15, 23, 42, 0.12)',
        },
        axisTicks: {
          color: isDark ? 'rgba(248, 250, 252, 0.12)' : 'rgba(15, 23, 42, 0.12)',
        },
      },
      yaxis: {
        labels: {
          formatter: (value: number) => this.formatRate(value),
          style: {
            colors: [isDark ? '#98a2b3' : '#667085'],
          },
        },
      },
      grid: {
        borderColor: isDark ? 'rgba(248, 250, 252, 0.08)' : 'rgba(15, 23, 42, 0.08)',
        strokeDashArray: 3,
      },
    };
  }

  private getErrorMessage(error: any, fallback: string): string {
    return error?.error?.detail ?? error?.error?.message ?? error?.message ?? fallback;
  }
}

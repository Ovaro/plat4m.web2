import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  ViewEncapsulation,
} from '@angular/core';

import { GridsterItem } from 'angular-gridster2';
import { Subscription } from 'rxjs';
import { CurrencyPipe } from '@angular/common';
import { DashboardService } from './dashboard.service';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';
import SharedModule from 'app/shared/shared.module';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexFill,
  ApexGrid,
  ApexPlotOptions,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  NgApexchartsModule,
} from 'ng-apexcharts';
import { SummaryChartSpec } from '../finance.model';

@Component({
  selector: 'standard-dashboard-panel',
  templateUrl: './standard.panel.component.html',
  styleUrl: './standard.panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  standalone: true,
  imports: [CurrencyPipe, SharedModule, TooltipModule, SkeletonModule, NgApexchartsModule],
})
export class StandardPanelComponent implements OnInit, OnDestroy, OnChanges {
  @Input()
  widget: any;
  @Input()
  resizeEvent: any;
  // resizeSub: Subscription;
  @Input()
  isLoading = false;

  resizeSub: Subscription | null = null;

  defaultCurrencyCode = 'AUD';
  summaryChartOptions: {
    series: ApexAxisChartSeries;
    chart: ApexChart;
    stroke: ApexStroke;
    fill: ApexFill;
    grid: ApexGrid;
    plotOptions: ApexPlotOptions;
    xaxis: ApexXAxis;
    yaxis: ApexYAxis;
    tooltip: ApexTooltip;
    colors: string[];
  } | null = null;

  constructor(
    private dashboardService: DashboardService,
    private changeDetectorRef: ChangeDetectorRef,
  ) {
    //this.resizeSub = {};
  }

  ngOnInit(): void {
    this.resizeSub = this.resizeEvent.subscribe((widget: any) => {
      if (widget === this.widget) {
        // or check id , type or whatever you have there
        // resize your widget, chart, map , etc.
        // console.log("resizeSub:" + widget);
        this.updateSummaryChart();
        this.changeDetectorRef.markForCheck();
      }
    });
    if (this.widget && this.widget.dataType) {
      //this.load();
      //this.widget.value = '-';
    }
  }

  ngOnChanges() {
    // eslint-disable-next-line no-console
    // console.log(`ngOnChanges`);
    this.updateSummaryChart();
  }

  load(): void {
    // eslint-disable-next-line no-console
    console.log('Loading snapshot');
    this.isLoading = true;

    this.dashboardService.getSnapshot(this.widget.dataType, this.widget.dataClass, this.widget.periodAgo).subscribe(
      response => {
        this.isLoading = false;
        // eslint-disable-next-line no-console
        console.log(`Response: ${JSON.stringify(response)}`);
        this.changeDetectorRef.markForCheck();

        this.widget.value = response.value;
        this.widget.comparisonValue = response.comparisonValue;
        // if(this.widget.negativeAsPositive) {
        //   this.widget.value = this.widget.value * -1;
        //   this.widget.comparisonValue = this.widget.comparisonValue *-1;
        // }
        console.log(`** Widget:`, JSON.stringify(this.widget));
      },
      error => {
        this.handleError(error);
      },
    );
  }

  handleError(error: any): void {
    this.isLoading = false;
    // Handle the error state here
    console.error('An error occurred:', error);
    // You can show an error message to the user or perform any other error handling logic
  }

  valueDeltaTooltip(): string | undefined {
    const current = this.displayValue(this.widget?.values?.[0]);
    const comparison = this.displayValue(this.widget?.comparisonValues?.[0]);
    if (current == null || comparison == null) {
      return undefined;
    }

    const delta = comparison === 0 ? null : ((current - comparison) / Math.abs(comparison)) * 100;
    const previousValue = this.formatMoney(comparison);
    if (delta == null || !Number.isFinite(delta)) {
      return `Was ${previousValue}`;
    }

    const sign = delta > 0 ? '+' : '';
    return `Was ${previousValue} (${sign}${delta.toFixed(1)}%)`;
  }

  ngOnDestroy(): void {
    this.resizeSub?.unsubscribe();
  }

  private displayValue(value: number | null | undefined): number | null {
    if (value == null || !Number.isFinite(value)) {
      return null;
    }

    return this.widget?.dataTypeSpecs?.[0]?.negativeAsPositive ? value * -1 : value;
  }

  private formatMoney(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: this.defaultCurrencyCode,
      maximumFractionDigits: 0,
    }).format(value);
  }

  private updateSummaryChart(): void {
    const spec = this.summaryChartSpec();
    const series = this.widget?.summaryChartSeries;
    if (!spec?.enabled || !series?.length) {
      this.summaryChartOptions = null;
      return;
    }

    const chartType = spec.chartType ?? 'area';
    const positiveTrend = (this.widget?.values?.[0] ?? 0) >= (this.widget?.comparisonValues?.[0] ?? 0);
    const primaryColor = positiveTrend ? '#15995a' : '#d64b4b';

    this.summaryChartOptions = {
      series,
      chart: {
        type: chartType,
        height: 48,
        sparkline: {
          enabled: true,
        },
        animations: {
          enabled: false,
        },
        toolbar: {
          show: false,
        },
      },
      stroke: {
        curve: spec.smooth && chartType !== 'bar' ? 'smooth' : 'straight',
        width: chartType === 'bar' ? 0 : 2,
      },
      fill: {
        opacity: chartType === 'bar' ? 0.72 : chartType === 'area' && spec.showFill !== false ? 0.22 : 0.95,
        type: chartType === 'area' && spec.showFill !== false ? 'gradient' : 'solid',
        gradient: {
          opacityFrom: 0.38,
          opacityTo: 0.02,
          shadeIntensity: 0.2,
        },
      },
      grid: {
        padding: {
          bottom: 0,
          left: 0,
          right: 0,
          top: 6,
        },
      },
      plotOptions: {
        bar: {
          borderRadius: 2,
          columnWidth: '62%',
          distributed: false,
        },
      },
      xaxis: {
        type: 'datetime',
        labels: {
          show: false,
        },
        axisBorder: {
          show: false,
        },
        axisTicks: {
          show: false,
        },
      },
      yaxis: {
        labels: {
          show: false,
        },
      },
      tooltip: {
        enabled: false,
      },
      colors: [primaryColor, '#6f8fb5', '#d59a3a', '#7c69b8'],
    };
  }

  private summaryChartSpec(): SummaryChartSpec | undefined {
    return this.widget?.dataTypeSpecs?.[0]?.summaryChartSpec;
  }
}

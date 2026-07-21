import { formatCurrency } from '@angular/common';
import { Component, effect, ElementRef, OnInit, viewChild, ViewChild } from '@angular/core';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexOptions,
  ApexXAxis,
  ApexYAxis,
  ChartComponent,
  ChartType,
  NgApexchartsModule,
} from 'ng-apexcharts';
import { FinanceResourceSnapshots } from '../finance.model';
import SharedModule from 'app/shared/shared.module';

export type HistoryChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  // title: ApexTitleSubtitle;
  stacked: boolean;
  options: ApexOptions;
  // tooltip: ApexTooltip;
  // annotations: ApexAnnotations;
};

@Component({
  selector: 'jhi-history-chart',
  imports: [SharedModule, NgApexchartsModule],
  templateUrl: './history-chart.component.html',
  styleUrl: './history-chart.component.scss',
})
export class HistoryChartComponent implements OnInit {
  // @ViewChild('historyChart', { static: false })
  // historyChart!: ChartComponent;
  historyChart = viewChild<ChartComponent>('historyChart');
  public historyChartOptions: Partial<HistoryChartOptions>;

  constructor() {
    this.historyChartOptions = {};
    effect(() => {
      const child = this.historyChart();
      if (child) {
        console.log('Child element is visible and available:', child);
      }
    });
  }

  ngOnInit(): void {
    const ptr = this;
    this.historyChartOptions = {
      //series: [{name: 'series1', data:[{x: '2020-06-01', y: 23},{x: '2020-07-01', y: 54},{x: '2020-08-01', y: 24},{x: '2020-09-01', y: 3},{x: '2020-10-01', y: 43},{x: '2020-11-01', y: 15}]}],
      series: [],
      chart: {
        height: '100%',
        type: 'area',
        stacked: true,

        animations: {
          enabled: false,
          dynamicAnimation: {
            enabled: true,
          },
        },
      },
      options: {
        stroke: {
          show: false,
          curve: 'straight',
        },
        markers: {
          size: 1,
          strokeOpacity: 0.1,
          fillOpacity: 0.1,
          hover: {
            size: 4,
            sizeOffset: 3,
          },
        },
        dataLabels: {
          enabled: false,
        },
      },
      xaxis: {
        type: 'datetime',
      },
      yaxis: {
        labels: {
          formatter: function (val: any) {
            // eslint-disable-next-line
            return ptr.currencyFormatter(val);
            //return "$"+(val).toFixed(2);
          },
        },
      },
    };
  }

  currencyFormatterMethod(element: any): string {
    return this.currencyFormatter(element.value);
  }

  currencyFormatter(value: any): string {
    let code: string | undefined;
    // if(this.account !== null ){
    //   code = this.account.currencyCode;
    // }
    if (code === undefined) {
      // TODO Fix hard coding here
      code = 'AUD';
    }

    //  = this.currencyPipe.transform(, 'symbol-narrow'
    const val = formatCurrency(value, 'en-AU', '$', 'AUD');
    if (val === null) {
      // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
      return '' + value;
    }

    return val;
  }

  updateChart(chartType: ChartType) {
    if (this.historyChartOptions.chart!.type !== chartType) {
      this.historyChartOptions.chart!.stackType = 'normal';
      this.historyChartOptions.chart!.stacked = true;
      this.historyChartOptions.chart!.type = chartType as ChartType;
      if (this.historyChart()) {
        this.historyChart()!.updateOptions(this.historyChartOptions);
      }
    }
  }

  createAllSeries(resourceSnapshots: FinanceResourceSnapshots[], chartType: ChartType, combineSeries: boolean) {
    let serieses: ApexAxisChartSeries = [];
    for (let i = 0; i < resourceSnapshots.length; i++) {
      // let name = "Test"; // resourceIds[i] ??
      let series = this.createSeries(resourceSnapshots[i], chartType);
      //if (atLeastOne) {
      if (series) {
        serieses.push(series);
      }
    }

    //let combineSeries = item.dataTypeSpecs[0].detailedPanelSpec?.combineSeries;
    if (combineSeries) {
      serieses = this.combineSeries(serieses, 'Combined');
    }

    return serieses;
  }

  createSeries(resourceSnapshot: FinanceResourceSnapshots | null, chartType: ChartType): any | null {
    let xy: [any?] = [];
    let series: any = { name: '', data: xy };
    let atLeastOne = false;

    // let resourceSnapshot: FinanceResourceSnapshots | null = this.findResourceSnapshot(resourceIds[i]);
    if (resourceSnapshot != null && resourceSnapshot.snapshots != null) {
      if (resourceSnapshot.name != null) {
        series.name = resourceSnapshot.name;
      }
      // Add value
      let lastKnownRateToBase = null;
      for (let k = 0; k < resourceSnapshot.snapshots.length; k++) {
        atLeastOne = true;
        if (resourceSnapshot.snapshots[k].fxToLocal) {
          lastKnownRateToBase = resourceSnapshot.snapshots[k].fxToLocal;
        }
        let value = resourceSnapshot.snapshots[k].value;
        if (lastKnownRateToBase) {
          value = value * lastKnownRateToBase;
        }

        //xy.push({ x: new Date(resourceSnapshot.snapshots[k].date).getTime(), y: value });
        xy.push({ x: resourceSnapshot.snapshots[k].date, y: value });
      }
    }
    return series;
  }

  combineSeries(inputSerieses: ApexAxisChartSeries, name: string) {
    let serieses: ApexAxisChartSeries = [];
    let xy: [any?] = [];
    let series = { name: name, data: xy };
    serieses.push(series);

    // Combine
    for (let i = 0; i < inputSerieses.length; i++) {
      let currentSeriesXY = inputSerieses[i].data;
      if (currentSeriesXY) {
        for (let j = 0; j < currentSeriesXY.length; j++) {
          let item = currentSeriesXY[j] as any;
          if (xy[j]) {
            if (item) {
              xy[j].y = xy[j].y + item.y;
            }
          } else {
            xy.push(item);
          }
        }
      }
    }

    return serieses;
  }

  setData(resourceSnapshots: FinanceResourceSnapshots[], chartType: ChartType, combineSeries: boolean) {
    let series = this.createAllSeries(resourceSnapshots, chartType, combineSeries);

    if (series) {
      this.historyChartOptions.series = series;
    }

    this.updateChart(chartType);
  }

  redrawChart() {
    if (this.historyChart()) {
      this.historyChart()!.updateOptions(this.historyChartOptions);
    }
  }
}

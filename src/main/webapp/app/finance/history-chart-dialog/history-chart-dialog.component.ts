import { Component, inject, ViewChild } from '@angular/core';
import { HistoryChartComponent } from '../history-chart/history-chart.component';
import { FinanceResourceSnapshots } from '../finance.model';
import SharedModule from 'app/shared/shared.module';
import { DialogModule } from 'primeng/dialog';
import { ChartType } from 'ng-apexcharts';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ThemeService } from 'app/layouts/main/theme.service';

@Component({
  selector: 'jhi-history-chart-dialog',
  imports: [SharedModule, DialogModule, HistoryChartComponent, ButtonModule, InputTextModule],
  templateUrl: './history-chart-dialog.component.html',
  styleUrl: './history-chart-dialog.component.scss',
})
export class HistoryChartDialogComponent {
  visible: boolean = false;
  name: string = '';

  historyChart!: HistoryChartComponent;
  @ViewChild('historyChart') set myDiv(historyChart: HistoryChartComponent) {
    this.historyChart = historyChart;
  }

  toSetResourceSnapshots: FinanceResourceSnapshots[] = [];
  toSetChartType: ChartType = 'bar';
  toSetCombineSeries: boolean = false;

  theme = 'light';
  private themeService = inject(ThemeService);

  constructor() {
    this.themeService.onChange.subscribe(event => {
      this.theme = event.theme;
      // if (this.historyChart) {
      //   this.historyChart.redrawChart();
      // }
    });
  }

  setData(name: string, resourceSnapshots: FinanceResourceSnapshots[], chartType: ChartType, combineSeries: boolean) {
    this.toSetResourceSnapshots = resourceSnapshots;
    this.toSetChartType = chartType;
    this.toSetCombineSeries = combineSeries;
    this.name = name;
  }

  showDialog() {
    this.visible = true;
  }

  onShow() {
    if (this.historyChart) {
      this.historyChart.setData(this.toSetResourceSnapshots, this.toSetChartType, this.toSetCombineSeries);
      this.redrawAfterLayout();
    }
  }

  onMaximize() {
    this.redrawAfterLayout();
  }

  private redrawAfterLayout(): void {
    window.setTimeout(() => {
      this.historyChart?.redrawChart();
    });
  }
}

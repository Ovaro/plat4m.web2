import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewEncapsulation,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { DashboardPanelDefinition } from './dashboard.types';
import { DataTypeSpec, FinanceNestedResource } from '../finance.model';

interface DataRefOption {
  value: string;
  label: string;
  group: string;
  raw?: boolean;
}

interface DataRefOptionGroup {
  group: string;
  options: DataRefOption[];
}

export interface DashboardPanelConfigSave {
  panel: DashboardPanelDefinition;
  widgetId: string;
  section: string;
}

@Component({
  selector: 'jhi-dashboard-panel-config-dialog',
  template: `
    <p-dialog
      header="Configure panel"
      [(visible)]="visible"
      [modal]="true"
      [style]="{ width: '42rem', maxWidth: '95vw' }"
      styleClass="dashboard-panel-config-dialog"
      [appendTo]="'body'"
    >
      <div class="dashboard-panel-config" *ngIf="working">
        <label>
          <span>Name</span>
          <input type="text" [(ngModel)]="working.name" />
        </label>

        <label>
          <span>Section heading</span>
          <input type="text" [(ngModel)]="workingSection" placeholder="Optional" />
        </label>

        <label>
          <span>Panel type</span>
          <select [(ngModel)]="working.panelType">
            <option value="standard">Standard</option>
            <option value="wide">Wide</option>
          </select>
        </label>

        <section class="dashboard-panel-config__spec" *ngFor="let spec of working.dataTypeSpecs; let i = index">
          <div class="dashboard-panel-config__spec-header">
            <div class="dashboard-panel-config__spec-title">
              <strong>{{ specHeading(i) }}</strong>
              <small *ngIf="specDescription(i)">{{ specDescription(i) }}</small>
            </div>
            <button type="button" class="dashboard-panel-config__link" (click)="removeSpec(i)" *ngIf="working.dataTypeSpecs.length > 1">
              Remove
            </button>
          </div>

          <label>
            <span>Title</span>
            <input type="text" [(ngModel)]="spec.title" />
          </label>

          <div class="dashboard-panel-config__field">
            <span>Data sources</span>
            <div class="dashboard-panel-config__data-picker">
              <input
                type="search"
                class="dashboard-panel-config__data-search"
                placeholder="Filter by name, type, symbol or currency"
                [ngModel]="dataRefFilterTerm(i)"
                (ngModelChange)="setDataRefFilter(i, $event)"
              />

              <div class="dashboard-panel-config__data-list" role="group" aria-label="Data sources">
                <ng-container *ngFor="let group of filteredDataRefGroups(i)">
                  <div class="dashboard-panel-config__data-group">{{ group.group }}</div>
                  <label class="dashboard-panel-config__data-option" *ngFor="let option of group.options">
                    <input
                      type="checkbox"
                      [checked]="hasDataRef(spec, option.value)"
                      (change)="toggleDataRef(spec, option.value, $any($event.target).checked)"
                    />
                    <span>{{ option.label }}</span>
                  </label>
                </ng-container>

                <div class="dashboard-panel-config__data-empty" *ngIf="!filteredDataRefGroups(i).length">No matching data sources</div>
              </div>
            </div>
          </div>

          <div class="dashboard-panel-config__refs" *ngIf="spec.dataType.length">
            <span *ngFor="let ref of spec.dataType" class="dashboard-panel-config__ref">{{ labelForRef(ref) }}</span>
          </div>

          <details class="dashboard-panel-config__advanced">
            <summary>Advanced refs</summary>
            <textarea rows="3" [ngModel]="joinDataRefs(spec)" (ngModelChange)="setDataRefs(spec, $event)"></textarea>
          </details>

          <label>
            <span>Aggregation</span>
            <select [(ngModel)]="spec.compositeDataTypeOperation">
              <option [ngValue]="null">Default sum</option>
              <option value="sum">Sum</option>
            </select>
          </label>

          <label class="dashboard-panel-config__check">
            <input type="checkbox" [(ngModel)]="spec.negativeAsPositive" />
            <span>Negative as positive</span>
          </label>

          <label class="dashboard-panel-config__check">
            <input type="checkbox" [(ngModel)]="spec.expandGroups" />
            <span>Expand resource groups</span>
          </label>

          <label>
            <span>Click-through chart</span>
            <select [ngModel]="spec.detailedPanelSpec?.chartType ?? ''" (ngModelChange)="setChartType(spec, $event)">
              <option value="">None</option>
              <option value="bar">Bar</option>
              <option value="area">Area</option>
            </select>
            <small>Used in the larger chart dialog when you click the panel.</small>
          </label>

          <label class="dashboard-panel-config__check" *ngIf="spec.detailedPanelSpec">
            <input type="checkbox" [(ngModel)]="spec.detailedPanelSpec.combineSeries" />
            <span>Combine click-through chart series</span>
          </label>

          <div class="dashboard-panel-config__subsection">
            <label class="dashboard-panel-config__check">
              <input
                type="checkbox"
                [ngModel]="spec.summaryChartSpec?.enabled ?? false"
                (ngModelChange)="setSummaryChartEnabled(spec, $event)"
              />
              <span>Show mini chart on this panel</span>
            </label>

            <ng-container *ngIf="spec.summaryChartSpec?.enabled">
              <label>
                <span>Mini chart type</span>
                <select [ngModel]="spec.summaryChartSpec?.chartType ?? 'area'" (ngModelChange)="setSummaryChartType(spec, $event)">
                  <option value="area">Area</option>
                  <option value="line">Line</option>
                  <option value="bar">Bar</option>
                </select>
                <small>Drawn inside the dashboard tile using the selected period.</small>
              </label>

              <label class="dashboard-panel-config__check">
                <input
                  type="checkbox"
                  [ngModel]="spec.summaryChartSpec?.combineSeries ?? true"
                  (ngModelChange)="setSummaryChartCombineSeries(spec, $event)"
                />
                <span>Combine mini chart series</span>
              </label>

              <label class="dashboard-panel-config__check">
                <input
                  type="checkbox"
                  [ngModel]="spec.summaryChartSpec?.smooth ?? true"
                  (ngModelChange)="setSummaryChartSmooth(spec, $event)"
                />
                <span>Smooth line</span>
              </label>

              <label class="dashboard-panel-config__check">
                <input
                  type="checkbox"
                  [ngModel]="spec.summaryChartSpec?.showFill ?? true"
                  (ngModelChange)="setSummaryChartShowFill(spec, $event)"
                />
                <span>Soft fill</span>
              </label>
            </ng-container>
          </div>
        </section>

        <button type="button" class="dashboard-panel-config__link" (click)="addSpec()">{{ addSpecLabel() }}</button>

        <div class="dashboard-panel-config__error" *ngIf="errorMessage">{{ errorMessage }}</div>
      </div>

      <ng-template pTemplate="footer">
        <button type="button" class="btn btn-outline-secondary" (click)="visible = false" [disabled]="saving">Cancel</button>
        <button
          type="button"
          class="btn btn-primary"
          (click)="save()"
          [disabled]="saving || !working?.name || !working?.dataTypeSpecs?.length"
        >
          {{ saving ? 'Saving...' : 'Save' }}
        </button>
      </ng-template>
    </p-dialog>
  `,
  styleUrl: './dashboard-panel-config-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [CommonModule, FormsModule, DialogModule],
})
export class DashboardPanelConfigDialogComponent implements OnChanges {
  @Input() resources: FinanceNestedResource[] = [];
  @Output() savePanel = new EventEmitter<DashboardPanelConfigSave>();

  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  visible = false;
  working: DashboardPanelDefinition | null = null;
  workingSection = '';
  saving = false;
  errorMessage = '';
  private workingWidgetId = '';
  dataRefOptions: DataRefOption[] = [];
  dataRefFilterTerms: string[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resources']) {
      this.rebuildDataRefOptions();
    }
  }

  open(panel: DashboardPanelDefinition, widgetId: string, section: string): void {
    this.working = structuredClone(panel);
    this.workingWidgetId = widgetId;
    this.workingSection = section;
    this.saving = false;
    this.errorMessage = '';
    this.dataRefFilterTerms = [];
    this.rebuildDataRefOptions();
    this.visible = true;
    this.changeDetectorRef.markForCheck();
  }

  addSpec(): void {
    this.working?.dataTypeSpecs.push({
      title: this.working?.panelType === 'wide' ? 'New detail' : 'New source',
      dataType: [],
      compositeDataTypeOperation: 'sum',
      negativeAsPositive: false,
      expandGroups: false,
    });
  }

  specHeading(index: number): string {
    if (this.working?.panelType !== 'wide') {
      return `Data source ${index + 1}`;
    }

    return index === 0 ? 'Primary metric (left side)' : `Detail ${index} (right side)`;
  }

  specDescription(index: number): string {
    if (this.working?.panelType !== 'wide') {
      return '';
    }

    return index === 0
      ? 'Controls the large value and optional mini chart on the left.'
      : 'Shown as one of up to four smaller detail rows on the right.';
  }

  addSpecLabel(): string {
    return this.working?.panelType === 'wide' ? 'Add right-side detail' : 'Add data source';
  }

  removeSpec(index: number): void {
    this.working?.dataTypeSpecs.splice(index, 1);
  }

  joinDataRefs(spec: DataTypeSpec): string {
    return spec.dataType?.join('\n') ?? '';
  }

  setDataRefs(spec: DataTypeSpec, value: string): void {
    spec.dataType = value
      .split(/[\n,]/)
      .map(item => item.trim())
      .filter(item => item.length > 0);
  }

  setSelectedDataRefs(spec: DataTypeSpec, value: string[] | string): void {
    spec.dataType = Array.isArray(value) ? value : [value];
  }

  setDataRefFilter(index: number, value: string): void {
    this.dataRefFilterTerms[index] = value;
  }

  dataRefFilterTerm(index: number): string {
    return this.dataRefFilterTerms[index] ?? '';
  }

  filteredDataRefGroups(index: number): DataRefOptionGroup[] {
    const term = (this.dataRefFilterTerms[index] ?? '').trim().toLowerCase();
    const groups = new Map<string, DataRefOption[]>();

    for (const option of this.dataRefOptions) {
      const searchableText = `${option.label} ${option.group} ${option.value}`.toLowerCase();
      if (term && !searchableText.includes(term)) {
        continue;
      }

      const groupOptions = groups.get(option.group) ?? [];
      groupOptions.push(option);
      groups.set(option.group, groupOptions);
    }

    return Array.from(groups, ([group, options]) => ({ group, options }));
  }

  hasDataRef(spec: DataTypeSpec, ref: string): boolean {
    return spec.dataType?.includes(ref) ?? false;
  }

  toggleDataRef(spec: DataTypeSpec, ref: string, selected: boolean): void {
    const current = spec.dataType ?? [];

    if (selected) {
      if (!current.includes(ref)) {
        spec.dataType = [...current, ref];
      }
      return;
    }

    spec.dataType = current.filter(item => item !== ref);
  }

  labelForRef(ref: string): string {
    return this.dataRefOptions.find(option => option.value === ref)?.label ?? `Custom: ${ref}`;
  }

  setChartType(spec: DataTypeSpec, value: string): void {
    if (!value) {
      spec.detailedPanelSpec = undefined;
      return;
    }
    spec.detailedPanelSpec = {
      type: 'chart',
      chartType: value,
      combineSeries: spec.detailedPanelSpec?.combineSeries ?? false,
    };
  }

  setSummaryChartEnabled(spec: DataTypeSpec, enabled: boolean): void {
    if (!enabled) {
      spec.summaryChartSpec = undefined;
      return;
    }
    spec.summaryChartSpec = {
      enabled: true,
      chartType: spec.summaryChartSpec?.chartType ?? 'area',
      combineSeries: spec.summaryChartSpec?.combineSeries ?? true,
      smooth: spec.summaryChartSpec?.smooth ?? true,
      showFill: spec.summaryChartSpec?.showFill ?? true,
    };
  }

  setSummaryChartType(spec: DataTypeSpec, chartType: 'line' | 'area' | 'bar'): void {
    this.setSummaryChartEnabled(spec, true);
    if (spec.summaryChartSpec) {
      spec.summaryChartSpec.chartType = chartType;
      if (chartType === 'bar') {
        spec.summaryChartSpec.smooth = false;
      }
    }
  }

  setSummaryChartCombineSeries(spec: DataTypeSpec, combineSeries: boolean): void {
    this.setSummaryChartEnabled(spec, true);
    if (spec.summaryChartSpec) {
      spec.summaryChartSpec.combineSeries = combineSeries;
    }
  }

  setSummaryChartSmooth(spec: DataTypeSpec, smooth: boolean): void {
    this.setSummaryChartEnabled(spec, true);
    if (spec.summaryChartSpec) {
      spec.summaryChartSpec.smooth = smooth;
    }
  }

  setSummaryChartShowFill(spec: DataTypeSpec, showFill: boolean): void {
    this.setSummaryChartEnabled(spec, true);
    if (spec.summaryChartSpec) {
      spec.summaryChartSpec.showFill = showFill;
    }
  }

  save(): void {
    if (!this.working) {
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.savePanel.emit({
      panel: this.working,
      widgetId: this.workingWidgetId,
      section: this.workingSection.trim(),
    });
  }

  saveSucceeded(): void {
    this.saving = false;
    this.visible = false;
    this.changeDetectorRef.markForCheck();
  }

  saveFailed(): void {
    this.saving = false;
    this.errorMessage = 'The panel configuration could not be saved. Please try again.';
    this.changeDetectorRef.markForCheck();
  }

  private rebuildDataRefOptions(): void {
    const options: DataRefOption[] = [
      { value: 'sumAccountType-bank', label: 'Account totals: Bank accounts', group: 'Totals' },
      { value: 'sumAccountType-cash', label: 'Account totals: Cash accounts', group: 'Totals' },
      { value: 'sumAccountType-credit', label: 'Account totals: Credit cards', group: 'Totals' },
      { value: 'sumAccountType-loan', label: 'Account totals: Loans', group: 'Totals' },
      { value: 'sumAccountType-investment', label: 'Account totals: Investment accounts', group: 'Totals' },
      { value: 'sumAccountType-asset', label: 'Account totals: Assets', group: 'Totals' },
      { value: 'sumAccountType-liability', label: 'Account totals: Liabilities', group: 'Totals' },
      { value: 'rg/AccountsByType', label: 'Group: Accounts by type', group: 'Groups' },
      { value: 'rg/AccountsByCurrency', label: 'Group: Accounts by currency', group: 'Groups' },
    ];

    this.appendResourceOptions(options, this.resources ?? [], []);

    const seen = new Set<string>();
    this.dataRefOptions = options.filter(option => {
      if (seen.has(option.value)) {
        return false;
      }
      seen.add(option.value);
      return true;
    });
  }

  private appendResourceOptions(options: DataRefOption[], resources: FinanceNestedResource[], parents: string[]): void {
    for (const resource of resources ?? []) {
      const path = [...parents, resource.name].filter(Boolean);
      const label = path.join(' / ');

      if (resource.id?.startsWith('rg/')) {
        options.push({ value: resource.id, label: `Group: ${label}`, group: 'Groups' });
      } else if (resource.id?.startsWith('customPortfolio:') || resource.type === 'customPortfolio') {
        options.push({ value: resource.id, label, group: 'Investment portfolios' });
      } else if (resource.id) {
        const suffix = [resource.symbol, resource.currencyCode, resource.type].filter(Boolean).join(' · ');
        options.push({
          value: resource.id,
          label: suffix ? `${label} (${suffix})` : label,
          group: resource.children?.length ? 'Resources' : 'Accounts and holdings',
        });
      }

      if (resource.id && resource.children?.length) {
        options.push({ value: `childrenOf:${resource.id}`, label: `Children of: ${label}`, group: 'Child groups' });
        this.appendResourceOptions(options, resource.children, path);
      }
    }
  }
}

import { CommonModule, CurrencyPipe, PercentPipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

/**
 * A component that will take care of item count statistics of a pagination.
 */
@Component({
  selector: 'jhi-delta-value',
  template: `<span
    *ngIf="comparison"
    [class.datawell-data-positive]="
      (!inverseValue && 1 - comparison! / value! > steadyVariance) || (inverseValue && 1 - comparison! / value! <= -steadyVariance)
    "
    [class.datawell-data-negative]="
      (!inverseValue && 1 - comparison! / value! <= -steadyVariance) || (inverseValue && 1 - comparison! / value! > steadyVariance)
    "
  >
    <span *ngIf="!pcOnly"> +{{ value! - comparison! | currency: currencyCode : 'symbol-narrow' }}&nbsp;&nbsp;</span
    ><span>
      <fa-icon
        *ngIf="(!inverseValue && 1 - comparison! / value! > steadyVariance) || (inverseValue && 1 - comparison! / value! > -steadyVariance)"
        icon="caret-up"
      ></fa-icon
      ><fa-icon
        *ngIf="
          (!inverseValue && 1 - comparison! / value! <= -steadyVariance) || (inverseValue && 1 - comparison! / value! <= -steadyVariance)
        "
        icon="caret-down"
      ></fa-icon>
      <span pTooltip="Value!" tooltipPosition="top">{{ 1 - comparison! / value! | percent: '1.1-1' }}</span>
    </span>
  </span> `,
  imports: [CommonModule, PercentPipe, CurrencyPipe, FontAwesomeModule],
})
export class DeltaValueComponent {
  steadyVariance = 0.01; // mark as steady if not more or less than this
  currencyCode = 'AU';
  pcOnly = false;
  inverseValue = false;

  /**
   * @param params  Contains parameters for component:
   *                    page          Current page number
   *                    totalItems    Total number of items
   *                    itemsPerPage  Number of items per page
   */
  @Input() set params(params: { value: number; comparison: number | undefined; currencyCode: string; pcOnly: boolean }) {
    this.currencyCode = params.currencyCode;
    this.value = params.value;
    this.comparison = params.comparison!;
    this.pcOnly = params.pcOnly;

    if (this.value < 0) {
      this.inverseValue = true;
    }
  }

  value?: number;
  comparison?: number;
}

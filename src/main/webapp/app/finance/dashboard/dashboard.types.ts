import { DataTypeSpec } from '../finance.model';

export type DashboardPanelScope = 'GLOBAL' | 'USER';

export interface DashboardPanelDefinition {
  id?: string;
  scope: DashboardPanelScope;
  name: string;
  panelType: string;
  dataTypeSpecs: DataTypeSpec[];
}

export interface DashboardWidgetInstance {
  id: string;
  panelDefinitionId: string;
  x: number;
  y: number;
  w: number;
  h: number;
  section?: string;
}

export interface DashboardConfig {
  id: string;
  name: string;
  active: boolean;
  widgets: DashboardWidgetInstance[];
  panelDefinitions: DashboardPanelDefinition[];
}

export interface DashboardLayoutUpdate {
  widgets: DashboardWidgetInstance[];
}

export interface DashboardGridItem extends DashboardWidgetInstance {
  cols: number;
  rows: number;
  type: string;
  selected: boolean;
  header: string;
  dataTypeSpecs: DataTypeSpec[];
  values?: Array<number | null>;
  comparisonValues?: Array<number | null>;
  [key: string]: any;
}

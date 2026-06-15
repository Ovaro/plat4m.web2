import { EventEmitter, Injectable, inject } from '@angular/core';
import { CookieService } from 'ngx-cookie';
import { colorSchemeDark, colorSchemeLight, themeAlpine, themeBalham, themeMaterial, themeQuartz, type Theme } from 'ag-grid-community';

export type AgGridThemeName = 'plat4m' | 'alpine' | 'balham' | 'material' | 'quartz';

export interface AgGridThemeOption {
  label: string;
  value: AgGridThemeName;
}

export const AG_GRID_THEME_OPTIONS: AgGridThemeOption[] = [
  { label: 'Plat4m', value: 'plat4m' },
  { label: 'Alpine', value: 'alpine' },
  { label: 'Balham', value: 'balham' },
  { label: 'Material', value: 'material' },
  { label: 'Quartz', value: 'quartz' },
];

@Injectable({ providedIn: 'root' })
export class AgGridThemeService {
  private static readonly STORAGE_KEY = 'plat4m-ag-grid-theme';
  private static readonly DEFAULT_THEME: AgGridThemeName = 'plat4m';

  readonly onChange = new EventEmitter<AgGridThemeName>();
  private readonly cookieService = inject(CookieService);

  theme(): AgGridThemeName {
    const storedTheme = this.cookieService.get(AgGridThemeService.STORAGE_KEY);
    return this.isValidTheme(storedTheme) ? storedTheme : AgGridThemeService.DEFAULT_THEME;
  }

  setTheme(theme: AgGridThemeName): void {
    this.cookieService.put(AgGridThemeService.STORAGE_KEY, theme);
    this.onChange.emit(theme);
  }

  getThemeClass(theme: AgGridThemeName, colorMode: string): string {
    return colorMode === 'dark' ? `ag-theme-${theme}-dark` : `ag-theme-${theme}`;
  }

  getThemeDefinition(theme: AgGridThemeName, colorMode: string = 'light'): Theme {
    const ibmPlexSans = [{ googleFont: 'IBM Plex Sans' }, 'IBM Plex Sans', 'sans-serif'];
    const colorScheme = colorMode === 'dark' ? colorSchemeDark : colorSchemeLight;

    switch (theme) {
      case 'plat4m':
        return this.getPlat4mThemeDefinition(colorMode, ibmPlexSans);
      case 'balham':
        return themeBalham.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
      case 'material':
        return themeMaterial.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
      case 'quartz':
        return themeQuartz.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
      case 'alpine':
      default:
        return themeAlpine.withPart(colorScheme).withParams({ fontFamily: ibmPlexSans });
    }
  }

  private isValidTheme(theme: string | null | undefined): theme is AgGridThemeName {
    return AG_GRID_THEME_OPTIONS.some(option => option.value === theme);
  }

  private getPlat4mThemeDefinition(colorMode: string, fontFamily: (string | { googleFont: string })[]): Theme {
    const isDark = colorMode === 'dark';
    const primary = '#1ca6f7';
    const textHigh = isDark ? '#f4f7fb' : '#1f2937';
    const textLow = isDark ? '#9ca8b7' : '#667085';
    const backgroundLow = isDark ? '#111827' : '#f7f9fc';
    const backgroundMed = isDark ? '#172033' : '#eef3f8';
    const backgroundHigh = isDark ? '#1f2a3d' : '#ffffff';
    const border = isDark ? 'rgba(244, 247, 251, 0.14)' : 'rgba(16, 24, 40, 0.12)';
    const borderStrong = isDark ? 'rgba(244, 247, 251, 0.2)' : 'rgba(16, 24, 40, 0.18)';
    const hover = isDark ? 'rgba(28, 166, 247, 0.13)' : 'rgba(28, 166, 247, 0.08)';
    const selected = isDark ? 'rgba(28, 166, 247, 0.2)' : 'rgba(28, 166, 247, 0.16)';

    return themeQuartz.withPart(isDark ? colorSchemeDark : colorSchemeLight).withParams({
      accentColor: primary,
      backgroundColor: backgroundHigh,
      browserColorScheme: isDark ? 'dark' : 'light',
      borderColor: border,
      borderRadius: 8,
      borderWidth: 1,
      cellFontSize: '0.72rem',
      cellHorizontalPadding: 10,
      cellTextColor: textHigh,
      chromeBackgroundColor: backgroundMed,
      dataBackgroundColor: backgroundHigh,
      fontFamily,
      fontSize: '0.72rem',
      foregroundColor: textHigh,
      headerBackgroundColor: isDark ? 'color-mix(in srgb, #1ca6f7 18%, #172033)' : 'rgba(28, 166, 247, 0.07)',
      headerFontSize: '0.72rem',
      headerFontWeight: 600,
      headerHeight: 34,
      headerRowBorder: `1px solid ${borderStrong}`,
      headerTextColor: textLow,
      iconButtonActiveColor: primary,
      iconButtonActiveBackgroundColor: selected,
      iconButtonHoverBackgroundColor: hover,
      iconButtonHoverColor: primary,
      iconColor: textLow,
      inputBackgroundColor: backgroundHigh,
      inputBorder: `1px solid ${border}`,
      inputFocusBorder: `1px solid ${primary}`,
      menuBackgroundColor: backgroundHigh,
      menuBorder: `1px solid ${borderStrong}`,
      menuShadow: isDark ? '0 14px 34px rgba(0, 0, 0, 0.46)' : '0 14px 34px rgba(16, 24, 40, 0.14)',
      menuTextColor: textHigh,
      oddRowBackgroundColor: isDark ? 'rgba(255, 255, 255, 0.018)' : 'rgba(16, 24, 40, 0.018)',
      panelBackgroundColor: backgroundHigh,
      pinnedColumnBorder: `1px solid ${borderStrong}`,
      rowBorder: `1px solid ${border}`,
      rowHeight: 31,
      rowHoverColor: hover,
      selectedRowBackgroundColor: selected,
      spacing: 6,
      subtleTextColor: textLow,
      textColor: textHigh,
      wrapperBackgroundColor: backgroundLow,
      wrapperBorder: `1px solid ${borderStrong}`,
      wrapperBorderRadius: 12,
    });
  }
}

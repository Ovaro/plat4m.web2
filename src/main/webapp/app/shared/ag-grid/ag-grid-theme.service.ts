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
    const textHigh = isDark ? 'rgba(255, 255, 255, 0.92)' : '#1f2937';
    const textLow = isDark ? 'rgba(255, 255, 255, 0.64)' : '#667085';
    const backgroundLow = isDark ? '#0d0f12' : '#f7f9fc';
    const backgroundMed = isDark ? '#191A1B' : '#eef3f8';
    const backgroundHigh = isDark ? '#151719' : '#ffffff';
    const backgroundRaised = isDark ? '#202225' : '#ffffff';
    const border = isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(16, 24, 40, 0.12)';
    const borderStrong = isDark ? 'rgba(255, 255, 255, 0.18)' : 'rgba(16, 24, 40, 0.18)';
    const hover = isDark ? 'rgba(255, 255, 255, 0.055)' : 'rgba(28, 166, 247, 0.08)';
    const selected = isDark ? 'rgba(28, 166, 247, 0.18)' : 'rgba(28, 166, 247, 0.16)';

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
      headerBackgroundColor: isDark ? backgroundMed : 'rgba(28, 166, 247, 0.07)',
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
      inputBackgroundColor: backgroundRaised,
      inputBorder: `1px solid ${border}`,
      inputFocusBorder: `1px solid ${primary}`,
      menuBackgroundColor: backgroundRaised,
      menuBorder: `1px solid ${borderStrong}`,
      menuShadow: isDark ? '0 14px 34px rgba(0, 0, 0, 0.46)' : '0 14px 34px rgba(16, 24, 40, 0.14)',
      menuTextColor: textHigh,
      oddRowBackgroundColor: isDark ? '#181a1d' : 'rgba(16, 24, 40, 0.018)',
      panelBackgroundColor: backgroundRaised,
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

import { EventEmitter, HostBinding, Inject, Injectable } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { DomSanitizer } from '@angular/platform-browser';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  public onChange: EventEmitter<ThemeChangeEvent> = new EventEmitter<ThemeChangeEvent>();

  @HostBinding('style.--text-color') color = '--app-colours-primary-text-light';

  private currentTheme = 'auto';

  constructor(@Inject(DOCUMENT) private document: Document) {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const ptr = this;

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (e) {
      if (ptr.currentTheme === 'auto') {
        ptr.sendThemeEvent(ptr.getOSTheme());
      }
    });
    // window.matchMedia('(prefers-color-scheme: light)').addEventListener('change',
    //     function(e) {
    //         if(ptr.currentTheme === 'auto'){
    //             ptr.sendThemeEvent( 'light');
    //         }
    // });
  }

  private syncDocumentTheme(theme: string): void {
    const normalizedTheme = theme === 'auto' ? this.getOSTheme() : theme;
    this.document.body.classList.remove('theme-light', 'theme-dark');
    this.document.documentElement.classList.remove('theme-light', 'theme-dark');
    this.document.body.classList.add(`theme-${normalizedTheme}`);
    this.document.documentElement.classList.add(`theme-${normalizedTheme}`);
  }

  switchTheme(theme: string): void {
    const themeLink = this.document.getElementById('app-theme') as HTMLLinkElement;
    themeLink.href = theme + '.css';
  }

  getOSTheme(): string {
    // if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    //     return 'dark';
    // }

    if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }

    return 'light';
  }

  theme(): string {
    if (this.currentTheme === '') {
      this.currentTheme = this.getOSTheme();
    }

    const resolvedTheme = this.currentTheme === 'auto' ? this.getOSTheme() : this.currentTheme;
    this.syncDocumentTheme(resolvedTheme);
    return resolvedTheme;
  }

  toggleTheme(): void {
    if (this.currentTheme === 'NOTSET' || this.currentTheme === 'auto') {
      this.currentTheme = this.getOSTheme();
    }

    if (this.currentTheme === 'light') {
      this.currentTheme = 'dark';
    } else {
      this.currentTheme = 'light';
    }

    this.sendThemeEvent(this.currentTheme);
  }

  public sendThemeEvent(theme: string): void {
    this.syncDocumentTheme(theme);
    // do something, then...
    this.onChange.emit({ theme });
  }

  // public updateCSSVariablesViaDom(sanitizer: DomSanitizer): void {
  //     //--app-colours-primary-text-'+[theme]+')
  //     sanitizer.bypassSecurityTrustStyle('--text-color: --app-colours-primary-text-"+this.theme()+")');
  // }
}

export class ThemeChangeEvent {
  theme: string;
  constructor(theme: string) {
    this.theme = theme;
  }
}

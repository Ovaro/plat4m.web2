import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { LANGUAGES } from 'app/config/language.constants';
import { Account } from 'app/core/auth/account.model';
import { AccountService } from 'app/core/auth/account.service';
import { AG_GRID_THEME_OPTIONS, AgGridThemeName, AgGridThemeService } from 'app/shared/ag-grid/ag-grid-theme.service';
import { AlertError } from 'app/shared/alert/alert-error';
import { FindLanguageFromKeyPipe, TranslateDirective } from 'app/shared/language';

const initialAccount: Account = {} as Account;

@Component({
  selector: 'jhi-settings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslateDirective, TranslateModule, FindLanguageFromKeyPipe, AlertError, ReactiveFormsModule],
  templateUrl: './settings.html',
})
export default class Settings implements OnInit {
  readonly success = signal(false);
  languages = LANGUAGES;
  agGridThemes = AG_GRID_THEME_OPTIONS;

  settingsForm = new FormGroup({
    firstName: new FormControl(initialAccount.firstName, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)],
    }),
    lastName: new FormControl(initialAccount.lastName, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(50)],
    }),
    email: new FormControl(initialAccount.email, {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(254), Validators.email],
    }),
    langKey: new FormControl(initialAccount.langKey, { nonNullable: true }),

    activated: new FormControl(initialAccount.activated, { nonNullable: true }),
    authorities: new FormControl(initialAccount.authorities, { nonNullable: true }),
    imageUrl: new FormControl(initialAccount.imageUrl, { nonNullable: true }),
    login: new FormControl(initialAccount.login, { nonNullable: true }),
    newAccount: new FormControl(initialAccount.newAccount, { nonNullable: true }),
    id: new FormControl(initialAccount.id, { nonNullable: true }),
    navType: new FormControl(initialAccount.langKey, { nonNullable: true }),
    agGridTheme: new FormControl<AgGridThemeName>('alpine', { nonNullable: true }),
  });

  private readonly accountService = inject(AccountService);
  private readonly translateService = inject(TranslateService);
  private readonly agGridThemeService = inject(AgGridThemeService);

  ngOnInit(): void {
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.settingsForm.patchValue(account);
      }
    });
    this.settingsForm.patchValue({ agGridTheme: this.agGridThemeService.theme() });
  }

  save(): void {
    this.success.set(false);

    const { agGridTheme, ...account } = this.settingsForm.getRawValue();
    this.accountService.save(account).subscribe({
      next: () => {
        this.success.set(true);

        this.accountService.authenticate(account);
        this.agGridThemeService.setTheme(agGridTheme);

        if (account.langKey !== this.translateService.getCurrentLang()) {
          this.translateService.use(account.langKey);
        }
      },
      error() {
        // Handled by interceptor.
      },
    });
  }
}

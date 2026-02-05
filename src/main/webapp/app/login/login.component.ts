import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { HttpErrorResponse } from '@angular/common/http';

import { LoginService } from 'app/login/login.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { Register } from 'app/shared/services/register.service';
import { PasswordResetInitService } from 'app/account/password-reset/init/password-reset-init.service';
// declare var require: any;
// const parseFullName = require('parse-full-name').parseFullName;
import { parseFullName } from 'parse-full-name';
import { ThemeChangeEvent, ThemeService } from 'app/layouts/main/theme.service';

import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

interface AccountType {
  name: string;
  value: string;
}

@Component({
  selector: 'jhi-login',
  imports: [SharedModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  animations: [
    // animation triggers go here
  ],
})
export default class LoginComponent {
  theme = 'light';
  // @ViewChild('username', { static: false })
  // usernameElement!: ElementRef;

  authenticationError = false;
  password = '';
  rememberMe = true;
  username = '';
  credentials: any;

  validEmail = false;
  checkedEmail = false;
  checkingEmail = false;
  emailFocus = false;
  checkError = false;
  userFound: string | null;

  checkingPassword = false;
  validPassword = false;
  checkedPassword = false;
  passwordFocus = false;

  formTitle = '';

  fullname: string;
  fullnameFocus = false;
  nameError: string;

  // accountTypes: AccountType[];
  // selectedAccountType: AccountType;

  betacode = '';
  betacodeFocus = false;
  // accountType = '';

  newPasswordFocus = false;
  newPassword: string;

  registering = false;
  registered = false;
  registrationError: string | null = null;

  forgotPasswordEmailFocus = false;
  forgotPasswordEmail: string | null = null;
  forgotPasswordError: string | null = null;
  forgotPasswordMode = false;
  recoveryEmailSent = false;

  @ViewChild('passwordField', { static: false }) passwordField!: ElementRef;
  @ViewChild('fullnameField', { static: false }) fullnameField!: ElementRef;
  @ViewChild('newPasswordField', { static: false }) newPasswordField!: ElementRef;
  @ViewChild('forgotPasswordEmailField', { static: false }) forgotPasswordEmailField!: ElementRef;

  constructor(
    private loginService: LoginService,
    private passwordResetInitService: PasswordResetInitService,
    private stateStorageService: StateStorageService,
    private registerService: Register,
    private themeService: ThemeService,
    private router: Router,
  ) {
    this.credentials = {};
    this.userFound = null;
    this.newPassword = '';
    this.fullname = '';
    this.nameError = '';
    this.determineFormTitle();

    // this.accountTypes = [
    //   {name: 'Worker', value: 'worker'},
    //   {name: 'Employer', value: 'employer'}
    // ];

    //this.selectedAccountType = this.accountTypes[0];
    this.theme = this.themeService.theme();
  }

  ngOnInit(): void {
    this.themeService.onChange.subscribe((event: ThemeChangeEvent) => {
      this.theme = event.theme;
    });
  }

  determineFormTitle(): void {
    if (this.forgotPasswordMode) {
      this.formTitle = 'Forgot Password';
    } else if (this.checkedEmail && this.userFound) {
      this.formTitle = 'Welcome back!';
    } else if (this.checkedEmail && !this.userFound && !this.registered) {
      this.formTitle = 'Welcome!';
    } else if (this.checkedEmail && !this.userFound && this.registered) {
      this.formTitle = 'Account Created!';
    } else {
      // Default
      this.formTitle = 'Sign in';
    }

    // console.log('Form Title: ' + this.formTitle);
  }

  cancel(): void {
    this.credentials = {
      username: null,
      password: null,
      rememberMe: true,
    };
    this.authenticationError = false;
  }

  onEmailChange(): void {
    this.userFound = null;
    this.checkedEmail = false;
    this.recoveryEmailSent = false;
    this.checkingEmail = false;
    this.authenticationError = false;
    this.password = '';
    this.validEmail = this.validateEmail(this.username);
    // console.log('Email valid? ' + this.validEmail);
    this.determineFormTitle();
  }

  onFullnameChange(): void {
    // Not Implemented.
  }

  isCreateAccountEnabled(): boolean {
    if (
      this.fullname.length > 5 &&
      this.newPassword.length > 7 &&
      this.betacode.length > 2
      // this.accountType &&
      // this.accountType !== ''
    ) {
      const spaceCount = this.fullname.split(' ').length - 1;
      if (spaceCount > 0) {
        const name = parseFullName(this.fullname);

        // console.log('Parsed Name: ' + JSON.stringify(name));
        if (name.error && name.error!.length > 0) {
          this.nameError = 'Invalid Name';
          // console.log('Error: ' + this.nameError);
          return false;
        }
        this.nameError = '';
        return true;
      } else {
        this.nameError = '';
        return false;
      }
    } else {
      this.nameError = '';
      return false;
    }
  }

  onPasswordChange(): void {
    this.authenticationError = false;
    this.checkingPassword = false;
    this.checkedPassword = false;
    if (this.password.length > 7) {
      this.validPassword = true;
    } else {
      this.validPassword = false;
    }
  }

  // ngAfterViewInit(): void {
  //   //this.usernameElement.nativeElement.focus();
  // }

  validateEmail(email: string): boolean {
    // console.log('Checking email address: ' + email);
    // eslint-disable-next-line no-useless-escape
    const re =
      /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
  }

  login(): void {
    this.recoveryEmailSent = false;
    if (this.checkedEmail && this.validPassword) {
      this.checkingPassword = true;

      this.loginService
        .login({
          username: this.username,
          password: this.password,
          rememberMe: this.rememberMe,
        })
        .subscribe(
          () => {
            this.authenticationError = false;
            this.checkingPassword = false;

            // this.eventManager.broadcast({
            //   name: 'authenticationSuccess',
            //   content: 'Sending Authentication Success'
            // });

            // previousState was set in the authExpiredInterceptor before being redirected to login modal.
            // since login is successful, go to stored previousState and clear previousState
            const redirect = this.stateStorageService.getUrl();

            // eslint-disable-next-line no-console
            console.log('Redirect URL is: {}', redirect);
            if (redirect && redirect !== '/login') {
              this.stateStorageService.storeUrl('');
              this.router.navigate([redirect]);
            } else {
              this.router.navigate(['/finance/dashboard']);
            }
          },
          err => {
            this.checkingPassword = false;
            this.authenticationError = true;
          },
        );

      this.determineFormTitle();
    } else if (!this.checkedEmail) {
      // console.log('Checking email');
      this.checkingEmail = true;
      this.loginService.checkExists(this.username).subscribe(
        () => {
          // console.log('Got: ' + JSON.stringify(data));
          this.checkError = false;
          this.checkingEmail = false;
          this.checkedEmail = true;
          this.userFound = this.username;

          this.focusPasswordWithDelay();
        },
        err => {
          this.checkingEmail = false;
          this.userFound = null;
          this.fullname = '';
          this.newPassword = '';
          if (err.status === 404) {
            // console.log('Email Address Not Found');
            this.checkError = false;
            this.checkedEmail = true;
            this.focusFullnameWithDelay();
          } else {
            // console.log('Error: ' + JSON.stringify(err));
            this.checkError = true;
            this.checkedEmail = false;
          }
          this.determineFormTitle();
        },
      );
    }
  }

  register(): void {
    // console.log('Registering...');
    const name = parseFullName(this.fullname);
    this.registrationError = null;
    const langKey = 'en';
    // "title":"","first":"Sdsd","middle":"Sdsd","last":"Ds","nick":"","suffix":""
    const registration = {
      password: this.newPassword,
      lastName: name.last,
      firstName: name.first,
      title: name.title,
      nickname: name.nick,
      suffix: name.suffix,
      betaCode: this.betacode,
      email: this.username,
      login: this.username,
      //      accountType: this.accountType,
      langKey,
    };
    this.registering = true;
    this.registerService.save(registration).subscribe(
      () => {
        this.registered = true;
        this.registering = false;
        this.determineFormTitle();
        setTimeout(() => {
          // console.log('Logging in...');
          this.checkedEmail = true;
          this.validPassword = true;
          this.password = this.newPassword;
          this.login();
        }, 1500);
      },
      response => this.processError(response),
    );
  }

  requestResetPassword(): void {
    this.router.navigate(['/reset', 'request']);
  }

  focusPasswordWithDelay(): void {
    // Cannot make truely generic function since passwordField is null until visible (hence the delay)
    setTimeout(() => {
      // console.log('Focusing...: ' + this.passwordField.nativeElement.valid);
      this.passwordField.nativeElement.focus();
      this.determineFormTitle();
    }, 600);
  }

  focusFullnameWithDelay(): void {
    // Cannot make truely generic function since field is null until visible (hence the delay)
    setTimeout(() => {
      // console.log('Focusing...: ' + this.fullnameField.nativeElement.valid);
      this.emailFocus = false;
      this.newPasswordFocus = false;
      this.newPasswordField.nativeElement.blur();
      this.fullnameField.nativeElement.focus();

      this.determineFormTitle();
    }, 600);
  }

  forgotPassword(): void {
    this.forgotPasswordMode = true;
    this.determineFormTitle();
    if (this.forgotPasswordEmail && this.validateEmail(this.forgotPasswordEmail)) {
      // console.log('Recovering account password:' + this.forgotPasswordEmail);
      this.forgotPasswordError = null;

      this.passwordResetInitService.save(this.forgotPasswordEmail).subscribe(
        () => {
          this.recoveryEmailSent = true;
          this.forgotPasswordMode = false;
          this.determineFormTitle();
          if (this.forgotPasswordEmail != null) {
            this.username = this.forgotPasswordEmail;
          }
          this.checkedEmail = false;
          this.checkedPassword = false;
        },
        response => {
          this.recoveryEmailSent = false;
          if (response.status === 400) {
            this.forgotPasswordError = 'Email address not found or has not been activitated';
          } else {
            this.forgotPasswordError = 'ERROR';
          }
        },
      );
    }
  }

  processBack(): void {
    if (this.forgotPasswordMode) {
      this.forgotPasswordMode = false;
      this.determineFormTitle();
    }
  }

  private processError(response: HttpErrorResponse): void {
    this.registering = false;
    // console.log('Error: ' + JSON.stringify(response));
    if (response.status === 400 && response.error === 'login already in use') {
      this.registrationError = 'Login already exists';
    } else if (response.status === 400 && response.error === 'wrong beta code') {
      this.registrationError = 'Incorrect BETA code';
    } else {
      this.registrationError = 'Internal Server Error';
    }
  }
}

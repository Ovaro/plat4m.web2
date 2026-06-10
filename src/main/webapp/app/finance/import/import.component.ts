import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { LANGUAGES } from 'app/config/language.constants';

import { FileUploadControl, FileUploadModule, FileUploadValidators } from '@iplab/ngx-file-upload';
import { FileService } from 'app/shared/services/file.service';
import { ImportService } from './import.service';
import { ImportStatusService } from './import-status.service';
import { Subscription } from 'rxjs';
import { ImportStatus } from './import-status.model';
import { Source } from '../finance.model';
import SharedModule from 'app/shared/shared.module';
import { NgApexchartsModule } from 'ng-apexcharts';
import { SelectModule } from 'primeng/select';

const initialAccount: Account = {} as Account;

@Component({
  selector: 'import',
  templateUrl: './import.component.html',
  imports: [SharedModule, FileUploadModule, NgApexchartsModule, SelectModule],
})
export class ImportComponent implements OnInit {
  success = false;
  isUploading = false;
  error = false;
  errorMessage = '';
  isDisabled = false;
  importPassword = '';

  finalStatus?: ImportStatus;
  statuses: ImportStatus[] = [];

  subscription?: Subscription;

  sources?: Source[];
  selectedSourceId = 1;
  selectedSource?: Source;

  allSources = [{ name: 'Microsoft Money', id: 1 }];

  public fileUploadControl = new FileUploadControl({ discardInvalid: true, disabled: false }, [
    FileUploadValidators.fileSize(52428800),
    FileUploadValidators.filesLimit(1),
    FileUploadValidators.accept(['.mny']),
  ]);

  public clear(): void {
    this.fileUploadControl.clear();
  }

  constructor(
    private importService: ImportService,
    private accountService: AccountService,
    private translateService: TranslateService,
    private importStatusService: ImportStatusService,
    private changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    //this.importStatusService.subscribe();
    this.importStatusService.connect();
    this.subscription = this.importStatusService.receive().subscribe((activity: ImportStatus) => {
      // eslint-disable-next-line no-console
      console.log('Got new status(1): ' + JSON.stringify(activity));
      this.showImportStatus(activity);
    });

    this.importService.getSources().subscribe({
      next: (res: Source[]) => {
        this.sources = res;
        this.onChangeType(null);
      },
      //error: () => (),
    });
  }

  ngOnDestroy(): void {
    this.importStatusService.unsubscribe();
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.importStatusService.disconnect();
  }

  onChangeType(event: any): void {
    // Find the type in the sources
    if (this.sources) {
      for (let index = 0; index < this.sources.length; index++) {
        if (this.sources[index].typeId === this.selectedSourceId) {
          this.selectedSource = this.sources[index];
        }
      }
    }
  }

  showImportStatus(newStatus: ImportStatus): void {
    // eslint-disable-next-line no-console
    console.log('Got new status(2): ' + JSON.stringify(newStatus));
    if (newStatus.importFinished) {
      this.finalStatus = newStatus;
      this.isUploading = false;
      this.success = !newStatus.error;
      this.error = !!newStatus.error;
      this.errorMessage = newStatus.error ?? '';
      if (this.success) {
        this.fileUploadControl.clear();
        this.importPassword = '';
      }
      this.changeDetectorRef.detectChanges();
      return;
    }

    let existingActivity = -1;

    for (let index = 0; index < this.statuses.length; index++) {
      if (this.statuses[index].taskName === newStatus.taskName) {
        existingActivity = index;
        this.statuses[index] = newStatus;
      }
    }

    if (existingActivity == -1) {
      this.statuses.push(newStatus);
    }

    this.changeDetectorRef.detectChanges();
  }

  upload(): void {
    // eslint-disable-next-line no-console
    console.log('Uploading...');
    this.success = false;
    this.error = false;
    this.errorMessage = '';
    this.statuses = [];
    this.finalStatus = undefined;
    const f = this.fileUploadControl.value[this.fileUploadControl.value.length - 1];
    if (f) {
      // eslint-disable-next-line no-console
      console.log('File:' + f.name);
      this.uploadToS3(f);
    }
  }

  uploadToS3(f: File) {
    // eslint-disable-next-line no-console
    console.log('Uploading to server...');
    this.isUploading = true;
    this.success = false;
    this.importService.uploadFile(f, this.importPassword.trim() || undefined).subscribe({
      complete: () => {
        console.log('Uploaded. Waiting for import status...');
      },
      error: err => {
        this.error = true;
        this.success = false;
        this.isUploading = false;
        this.statuses = [];
        this.finalStatus = undefined;
        this.errorMessage = this.getUploadErrorMessage(err);
        this.changeDetectorRef.detectChanges();
      },
    });
  }

  isImportBusy(): boolean {
    return this.isUploading || (this.statuses.length > 0 && !this.finalStatus && !this.error);
  }

  private getUploadErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 413) {
        return 'The selected file is too large to upload.';
      }

      if (typeof err.error === 'string' && err.error.trim()) {
        return err.error;
      }

      if (err.error?.title) {
        return err.error.title as string;
      }

      if (err.message) {
        return err.message;
      }
    }

    return 'The file upload failed. Please try again.';
  }
}

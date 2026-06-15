import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AlertError } from 'app/shared/alert/alert-error';

import { ApiKeysService } from './api-keys.service';
import { CreatedUserApiKeyResponse, UserApiKey } from './api-keys.types';

@Component({
  selector: 'jhi-api-keys-settings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, AlertError],
  templateUrl: './api-keys.component.html',
  styleUrl: './api-keys.component.scss',
})
export class ApiKeysComponent implements OnInit {
  readonly apiKeys = signal<UserApiKey[]>([]);
  readonly createdToken = signal<CreatedUserApiKeyResponse | null>(null);
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly removingKeyId = signal<string | null>(null);

  readonly createForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    expiresAt: new FormControl(this.getDefaultExpiryDate(), { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly apiKeysService = inject(ApiKeysService);

  ngOnInit(): void {
    this.loadApiKeys();
  }

  createApiKey(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.createdToken.set(null);

    this.apiKeysService
      .create({
        name: this.createForm.controls.name.getRawValue().trim(),
        expiresAt: this.toExpiryInstant(this.createForm.controls.expiresAt.getRawValue()),
      })
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe(result => {
        this.createdToken.set(result);
        this.createForm.reset({
          name: '',
          expiresAt: this.getDefaultExpiryDate(),
        });
        this.loadApiKeys();
      });
  }

  removeApiKey(id: string): void {
    this.removingKeyId.set(id);
    this.apiKeysService
      .remove(id)
      .pipe(finalize(() => this.removingKeyId.set(null)))
      .subscribe(() => {
        this.createdToken.update(current => (current?.apiKey.id === id ? null : current));
        this.loadApiKeys();
      });
  }

  private loadApiKeys(): void {
    this.isLoading.set(true);
    this.apiKeysService
      .list()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe(apiKeys => this.apiKeys.set(apiKeys));
  }

  private getDefaultExpiryDate(): string {
    const date = new Date();
    date.setMonth(date.getMonth() + 3);
    return this.toDateInputValue(date);
  }

  private toExpiryInstant(dateValue: string): string {
    const [year, month, day] = dateValue.split('-').map(Number);
    return new Date(year, month - 1, day, 23, 59, 59, 999).toISOString();
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}

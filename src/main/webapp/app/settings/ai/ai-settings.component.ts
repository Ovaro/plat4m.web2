import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AlertError } from 'app/shared/alert/alert-error';
import { AiModelOption } from './ai-model-option.model';
import { AiSettingsService } from './ai-settings.service';

@Component({
  selector: 'jhi-ai-settings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, AlertError],
  templateUrl: './ai-settings.component.html',
  styleUrl: './ai-settings.component.scss',
})
export class AiSettingsComponent implements OnInit {
  private static readonly DEFAULT_MODEL = 'gemini-flash-latest';

  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly success = signal(false);
  readonly hasSavedApiKey = signal(false);
  readonly maskedApiKey = signal<string | null>(null);
  readonly modelOptions = signal<AiModelOption[]>([]);

  readonly settingsForm = new FormGroup({
    defaultModel: new FormControl(AiSettingsComponent.DEFAULT_MODEL, {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
    apiKey: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(512)] }),
    clearApiKey: new FormControl(false, { nonNullable: true }),
  });

  private readonly aiSettingsService = inject(AiSettingsService);

  ngOnInit(): void {
    this.loadModels();
    this.loadSettings();
  }

  save(): void {
    if (this.settingsForm.invalid) {
      this.settingsForm.markAllAsTouched();
      return;
    }

    this.success.set(false);
    this.isSaving.set(true);

    this.aiSettingsService
      .updateSettings({
        defaultModel: this.settingsForm.controls.defaultModel.getRawValue().trim(),
        apiKey: this.settingsForm.controls.apiKey.getRawValue().trim() || null,
        clearApiKey: this.settingsForm.controls.clearApiKey.getRawValue(),
        modelOverrides: {},
      })
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe(settings => {
        this.hasSavedApiKey.set(settings.hasApiKey);
        this.maskedApiKey.set(settings.maskedApiKey);
        this.ensureModelOption(settings.defaultModel);
        this.settingsForm.patchValue({
          defaultModel: settings.defaultModel,
          apiKey: '',
          clearApiKey: false,
        });
        this.success.set(true);
      });
  }

  private loadSettings(): void {
    this.isLoading.set(true);
    this.aiSettingsService
      .getSettings()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe(settings => {
        this.hasSavedApiKey.set(settings.hasApiKey);
        this.maskedApiKey.set(settings.maskedApiKey);
        this.ensureModelOption(settings.defaultModel);
        this.settingsForm.patchValue({
          defaultModel: settings.defaultModel,
          apiKey: '',
          clearApiKey: false,
        });
      });
  }

  private loadModels(): void {
    this.aiSettingsService.getModels().subscribe(models => {
      this.modelOptions.set(this.withDefaultModel(models));
      this.ensureModelOption(this.settingsForm.controls.defaultModel.getRawValue());
    });
  }

  private withDefaultModel(models: AiModelOption[]): AiModelOption[] {
    const deduped = new Map<string, AiModelOption>();
    deduped.set(AiSettingsComponent.DEFAULT_MODEL, {
      id: AiSettingsComponent.DEFAULT_MODEL,
      label: 'Gemini Flash Latest',
      description: 'Rolling alias for the latest Gemini Flash model.',
    });

    for (const model of models) {
      if (!deduped.has(model.id)) {
        deduped.set(model.id, model);
      }
    }

    return Array.from(deduped.values());
  }

  private ensureModelOption(modelId: string): void {
    if (!modelId) {
      return;
    }

    const existingOptions = this.modelOptions();
    if (existingOptions.some(option => option.id === modelId)) {
      return;
    }

    this.modelOptions.set([
      ...existingOptions,
      {
        id: modelId,
        label: modelId,
        description: null,
      },
    ]);
  }
}

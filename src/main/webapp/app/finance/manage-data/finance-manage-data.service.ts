import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { FinancialTransaction } from '../finance.model';
import { ManagedCategory, ManagedCategoryUpdate, ManagedPayee, ManagedPayeeUpdate } from './finance-manage-data.types';

@Injectable({ providedIn: 'root' })
export class FinanceManageDataService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  getCategories(): Observable<ManagedCategory[]> {
    return this.http.get<ManagedCategory[]>(this.applicationConfigService.getEndpointFor('api/categories'));
  }

  createCategory(update: ManagedCategoryUpdate): Observable<ManagedCategory> {
    return this.http.post<ManagedCategory>(this.applicationConfigService.getEndpointFor('api/categories'), update);
  }

  updateCategory(id: string, update: ManagedCategoryUpdate): Observable<ManagedCategory> {
    return this.http.put<ManagedCategory>(this.applicationConfigService.getEndpointFor(`api/categories/${id}`), update);
  }

  deleteCategory(id: string): Observable<null> {
    return this.http.delete<null>(this.applicationConfigService.getEndpointFor(`api/categories/${id}`));
  }

  getCategoryTransactions(id: string): Observable<FinancialTransaction[]> {
    return this.http.get<FinancialTransaction[]>(this.applicationConfigService.getEndpointFor(`api/categories/${id}/transactions`));
  }

  getPayees(includeHidden: boolean): Observable<ManagedPayee[]> {
    return this.http.get<ManagedPayee[]>(this.applicationConfigService.getEndpointFor('api/payees'), {
      params: { includeHidden },
    });
  }

  getPayeeTransactions(id: string): Observable<FinancialTransaction[]> {
    return this.http.get<FinancialTransaction[]>(this.applicationConfigService.getEndpointFor(`api/payees/${id}/transactions`));
  }

  createPayee(update: ManagedPayeeUpdate): Observable<ManagedPayee> {
    return this.http.post<ManagedPayee>(this.applicationConfigService.getEndpointFor('api/payees'), update);
  }

  updatePayee(id: string, update: ManagedPayeeUpdate): Observable<ManagedPayee> {
    return this.http.put<ManagedPayee>(this.applicationConfigService.getEndpointFor(`api/payees/${id}`), update);
  }

  deletePayee(id: string): Observable<null> {
    return this.http.delete<null>(this.applicationConfigService.getEndpointFor(`api/payees/${id}`));
  }
}

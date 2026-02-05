import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { Source } from '../finance.model';

@Injectable({
  providedIn: 'root',
})
export class ImportService {
  private resourceUrl = 'api/uploadURL';

  constructor(
    private http: HttpClient,
    private applicationConfigService: ApplicationConfigService,
  ) {}

  uploadURL(): Observable<any> {
    return this.http.get(`${this.resourceUrl}`, { observe: 'response' });
  }

  putImageS3(body: string, presignedUrl: string): Observable<any> {
    // const options = new HttpHeaders();
    const httpOptions = {
      headers: new HttpHeaders({ 'Content-Type': 'image/jpeg', 'Content-Encoding': 'base64' }),
    };
    return this.http.put(presignedUrl, this.convertToBlob(body), httpOptions);
  }

  putFileS3(f: File, presignedUrl: string): Observable<any> {
    // const options = new HttpHeaders();
    const httpOptions = {
      headers: new HttpHeaders(),
    };
    return this.http.put(presignedUrl, f, httpOptions);
  }

  uploadFile(f: File) {
    // const httpOptions = {
    //   headers: new HttpHeaders({
    //     "Content-Type": "multipart/form-data" // 👈
    //    })
    // };
    let formData: FormData = new FormData();
    formData.append('file', f);
    //formData.append("id", "debug");

    //return this.http.post('/api/importFile', formData, httpOptions);
    return this.http.post('/api/importFile?password=password', formData);
  }

  makePublic(id: string): Observable<any> {
    return this.http.get('/api/publicURL/' + id, { observe: 'response' });
  }

  convertToBlob(base64Str: string) {
    const binary = atob(base64Str.split(',')[1]);
    const array: number[] = [];
    for (let i = 0; i < binary.length; i++) {
      array.push(binary.charCodeAt(i));
    }

    return new Blob([new Uint8Array(array)], { type: 'image/jpeg' });
  }

  getSources(): Observable<Source[]> {
    return this.http.get<Source[]>(this.applicationConfigService.getEndpointFor('api/source'));
  }
}

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class FileService {
  private resourceUrl = 'api/uploadURL';

  constructor(private http: HttpClient) {}

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
}

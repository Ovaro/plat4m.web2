export class ImportStatus {
  constructor(
    public id: string,
    public taskNumber: number,
    public taskName: string,
    public taskFinished: boolean,
    public importFinished: boolean,
    public error: string,
    public numInput: number,
    public numCreated: number,
    public numDeleted: number,
    public numUpdated: number,
    public duration: number,
  ) {}
}

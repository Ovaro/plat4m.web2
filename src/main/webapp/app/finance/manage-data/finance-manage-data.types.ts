export interface ManagedCategory {
  id: string;
  name: string;
  displayName: string;
  parentId: string | null;
  parentName: string | null;
  classificationId: number;
  level: number | null;
  comment: string | null;
  hasChildren: boolean;
}

export interface ManagedCategoryUpdate {
  name: string;
  parentId: string | null;
  classificationId: number;
  comment: string | null;
}

export interface ManagedPayee {
  id: string;
  name: string;
  parentId: string | null;
  hidden: boolean | null;
  childCount: number;
  childNames: string[];
}

export interface ManagedPayeeUpdate {
  name: string;
  parentId: string | null;
  hidden: boolean;
}

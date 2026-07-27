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
  systemCategory: boolean;
  knownCategoryKey: string | null;
  platformDefaultCategory: boolean;
  templateCategory: boolean;
  hidden: boolean;
}

export interface ManagedCategoryUpdate {
  name: string;
  parentId: string | null;
  classificationId: number;
  comment: string | null;
  knownCategoryKey: string | null;
  hidden: boolean;
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

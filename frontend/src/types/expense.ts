export type SplitType = 'EQUAL' | 'PERCENTAGE' | 'AMOUNT';

export interface ParticipantShare {
  userId: string;
  value?: string | number;
}

export interface AddExpensePayload {
  title: string;
  amount: string | number;
  paidBy: string;
  splitType: SplitType;
  expenseDate: string;
  participants: ParticipantShare[];
}

export interface ExpenseShare {
  userId: string;
  shareAmount: string;
}

export interface Expense {
  id: string;
  groupId: string;
  paidBy: string;
  title: string;
  amount: string;
  splitType: SplitType;
  expenseDate: string;
  createdAt: string;
  shares: ExpenseShare[];
}

export interface ExpensePage {
  content: Expense[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MemberBalance {
  userId: string;
  displayName: string;
  balance: string;
}

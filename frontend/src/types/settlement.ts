export interface SettlementSuggestion {
  payerId: string;
  payerName: string;
  payeeId: string;
  payeeName: string;
  amount: string;
}

export interface RecordSettlementPayload {
  payerId: string;
  payeeId: string;
  amount: string | number;
}

export interface Settlement {
  id: string;
  groupId: string;
  payerId: string;
  payeeId: string;
  amount: string;
  status: string;
  settledAt: string;
}

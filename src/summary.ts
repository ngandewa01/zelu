import { Row } from './storage';

export interface MonthTotal {
  month: string; // YYYY-MM
  income: number; // income + refunds
  expense: number;
  net: number;
}

// Transfers between your own accounts are excluded from every total.
export function monthlyTotals(rows: Row[]): MonthTotal[] {
  const byMonth: Record<string, { income: number; expense: number }> = {};
  for (const r of rows) {
    const key = r.date.slice(0, 7);
    byMonth[key] ??= { income: 0, expense: 0 };
    if (r.kind === 'income' || r.kind === 'refund') {
      byMonth[key].income += r.amount;
    } else if (r.kind === 'expense') {
      byMonth[key].expense += r.amount;
    }
  }
  return Object.keys(byMonth)
    .sort()
    .map(month => ({
      month,
      income: byMonth[month].income,
      expense: byMonth[month].expense,
      net: byMonth[month].income - byMonth[month].expense,
    }));
}

export function expenseByCategory(rows: Row[]): [string, number][] {
  const totals: Record<string, number> = {};
  for (const r of rows) {
    if (r.kind === 'expense') {
      totals[r.category] = (totals[r.category] ?? 0) + r.amount;
    }
  }
  return Object.entries(totals).sort((a, b) => b[1] - a[1]);
}

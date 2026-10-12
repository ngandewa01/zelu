import { REFUND, Txn } from './parser';

export type Kind = 'income' | 'expense' | 'transfer' | 'refund';

// Category -> keywords matched against counterparty and SMS text (case-insensitive).
export const CATEGORIES: Record<string, string[]> = {
  Food: ['swiggy', 'zomato', 'restaurant', 'cafe', 'bakery'],
  Groceries: ['blinkit', 'zepto', 'bigbasket', 'dmart', 'instamart'],
  Transport: ['uber', 'ola', 'rapido', 'irctc', 'metro', 'petrol', 'fuel'],
  Shopping: ['amazon', 'flipkart', 'myntra', 'meesho'],
  Bills: ['airtel', 'jio', 'electricity', 'recharge', 'broadband', 'gas'],
  Entertainment: ['netflix', 'spotify', 'bookmyshow', 'hotstar'],
};

export function classify(
  t: Txn,
  ownAccounts: string[],
): { kind: Kind; category: string } {
  const cp = (t.counterparty ?? '').toLowerCase();

  // Money moving between your own accounts is not income or spending.
  if (cp && ownAccounts.some(o => o && cp.includes(o.toLowerCase()))) {
    return { kind: 'transfer', category: 'Transfer' };
  }

  if (t.direction === 'credit') {
    return REFUND.test(t.raw)
      ? { kind: 'refund', category: 'Refund' }
      : { kind: 'income', category: 'Income' };
  }

  const hay = `${cp} ${t.raw}`.toLowerCase();
  for (const [category, words] of Object.entries(CATEGORIES)) {
    if (words.some(w => hay.includes(w))) {
      return { kind: 'expense', category };
    }
  }
  return { kind: 'expense', category: 'Uncategorized' };
}

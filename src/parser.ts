// Parses bank / UPI SMS alerts into transactions.
// Patterns are deliberately loose because bank SMS formats vary.

export type Direction = 'debit' | 'credit';

export interface Txn {
  id: string;
  date: string; // YYYY-MM-DD (IST)
  amount: number;
  direction: Direction;
  counterparty: string | null;
  ref: string | null;
  raw: string;
  source: string;
}

const AMOUNT = /(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)/i;
const DEBIT = /\b(debited|sent|paid|spent|withdrawn)\b/i;
const CREDIT = /\b(credited|received|deposited|refunded?)\b/i;
export const REFUND = /\brefund/i;
// OTPs, offers and loan promos contain amounts too; they are not transactions.
const SKIP = /\b(otp|one[- ]time password|do not share|offer|pre-?approved|loan)\b/i;
const REF =
  /\b(?:UPI\s*Ref(?:erence)?(?:\s*No\.?)?|RRN|Ref(?:\s*No\.?)?)\s*[:#-]?\s*(\d{6,})/i;
const DATE = /\b(\d{1,2})[-/](\d{1,2})[-/](\d{2}|\d{4})\b/;
const CP =
  /\b(?:to|from|by)\s+(?:VPA\s+)?([\w.\-]+@[\w.\-]+|[A-Za-z][A-Za-z .&'-]{1,40}?)(?=\s+(?:on|UPI|Ref|via|of)\b|[.,;]|$)/i;

const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

export function parseSms(
  body: string,
  receivedMs: number | null,
  source: string,
): Txn | null {
  const text = body.replace(/\s+/g, ' ').trim();
  if (!text || SKIP.test(text)) {
    return null;
  }

  const amt = AMOUNT.exec(text);
  if (!amt) {
    return null;
  }
  const amount = parseFloat(amt[1].replace(/,/g, ''));

  const d = DEBIT.exec(text);
  const c = CREDIT.exec(text);
  let direction: Direction;
  if (d && (!c || d.index < c.index)) {
    direction = 'debit';
  } else if (c) {
    direction = 'credit';
  } else {
    return null;
  }

  const date = parseDate(text, receivedMs);
  const refM = REF.exec(text);
  const ref = refM ? refM[1] : null;
  const cpM = CP.exec(text);
  const counterparty = cpM ? cpM[1].trim() : null;

  const id = ref
    ? `ref-${ref}`
    : `sms-${hash(`${date}|${amount}|${direction}|${text}`)}`;

  return { id, date, amount, direction, counterparty, ref, raw: text, source };
}

function parseDate(text: string, receivedMs: number | null): string {
  const m = DATE.exec(text);
  if (m) {
    const dd = Number(m[1]);
    const mm = Number(m[2]);
    let yy = Number(m[3]);
    if (yy < 100) {
      yy += 2000;
    }
    const dt = new Date(Date.UTC(yy, mm - 1, dd));
    if (dt.getUTCDate() === dd && dt.getUTCMonth() === mm - 1) {
      return dt.toISOString().slice(0, 10);
    }
  }
  const base = receivedMs ?? Date.now();
  return new Date(base + IST_OFFSET_MS).toISOString().slice(0, 10);
}

// Non-cryptographic 53-bit string hash (cyrb53) used to dedupe SMS without a ref.
function hash(s: string): string {
  let h1 = 0xdeadbeef;
  let h2 = 0x41c6ce57;
  for (let i = 0; i < s.length; i++) {
    const ch = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return (4294967296 * (2097151 & h2) + (h1 >>> 0)).toString(36);
}

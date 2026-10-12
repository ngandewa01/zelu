import { classify } from './classify';
import { parseSms } from './parser';
import { RawSms } from './sms';
import { loadSettings, Row, saveRows, splitList } from './storage';

// Serialize ingests so a background queue drain and a manual import
// cannot overwrite each other's writes.
let lock: Promise<unknown> = Promise.resolve();

export function ingest(
  raw: RawSms[],
): Promise<{ found: number; added: number }> {
  const run = lock.then(() => doIngest(raw));
  lock = run.catch(() => undefined);
  return run;
}

async function doIngest(
  raw: RawSms[],
): Promise<{ found: number; added: number }> {
  const settings = await loadSettings();
  const senders = splitList(settings.bankSenders).map(s => s.toLowerCase());
  const own = splitList(settings.ownAccounts);

  const rows: Row[] = [];
  for (const msg of raw) {
    const address = (msg.address ?? '').toLowerCase();
    // With bank senders configured, ignore personal chats entirely.
    if (senders.length > 0 && !senders.some(s => address.includes(s))) {
      continue;
    }
    const txn = parseSms(msg.body ?? '', msg.date ?? null, `phone:${msg.address}`);
    if (!txn) {
      continue;
    }
    const { kind, category } = classify(txn, own);
    rows.push({ ...txn, kind, category });
  }

  const added = await saveRows(rows);
  return { found: rows.length, added };
}

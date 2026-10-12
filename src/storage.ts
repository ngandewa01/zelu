import AsyncStorage from '@react-native-async-storage/async-storage';
import { Txn } from './parser';
import { Kind } from './classify';

export interface Row extends Txn {
  kind: Kind;
  category: string;
}

export interface Settings {
  bankSenders: string; // comma separated, e.g. "HDFCBK, SBIINB"
  ownAccounts: string; // comma separated UPI IDs or names
}

const TXN_KEY = 'walnut.txns';
const SETTINGS_KEY = 'walnut.settings';

export async function loadRows(): Promise<Row[]> {
  const raw = await AsyncStorage.getItem(TXN_KEY);
  const map: Record<string, Row> = raw ? JSON.parse(raw) : {};
  return Object.values(map).sort((a, b) => b.date.localeCompare(a.date));
}

// Returns how many rows were new. Existing ids are kept, so repeat imports are safe.
export async function saveRows(newRows: Row[]): Promise<number> {
  const raw = await AsyncStorage.getItem(TXN_KEY);
  const map: Record<string, Row> = raw ? JSON.parse(raw) : {};
  let added = 0;
  for (const r of newRows) {
    if (!map[r.id]) {
      map[r.id] = r;
      added++;
    }
  }
  await AsyncStorage.setItem(TXN_KEY, JSON.stringify(map));
  return added;
}

export async function loadSettings(): Promise<Settings> {
  const raw = await AsyncStorage.getItem(SETTINGS_KEY);
  return raw ? JSON.parse(raw) : { bankSenders: '', ownAccounts: '' };
}

export async function saveSettings(s: Settings): Promise<void> {
  await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
}

export function splitList(s: string): string[] {
  return s
    .split(',')
    .map(x => x.trim())
    .filter(x => x.length > 0);
}

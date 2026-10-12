import React, { useCallback, useEffect, useState } from 'react';
import {
  AppState,
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
} from 'react-native';
import { ingest } from './src/ingest';
import { drainQueue, readInbox, requestSmsPermissions } from './src/sms';
import { loadRows, loadSettings, Row, saveSettings } from './src/storage';
import { expenseByCategory, monthlyTotals } from './src/summary';

export default function App() {
  const [rows, setRows] = useState<Row[]>([]);
  const [senders, setSenders] = useState('');
  const [own, setOwn] = useState('');
  const [status, setStatus] = useState('');

  const refresh = useCallback(async () => {
    setRows(await loadRows());
  }, []);

  // Pick up SMS that arrived while the app was closed.
  const syncQueue = useCallback(async () => {
    try {
      const queued = await drainQueue();
      if (queued.length > 0) {
        const r = await ingest(queued);
        setStatus(`Added ${r.added} new from incoming SMS.`);
      }
    } catch (e) {
      // Native module not linked or permission not granted yet; the import button shows the error.
    }
    await refresh();
  }, [refresh]);

  useEffect(() => {
    (async () => {
      const s = await loadSettings();
      setSenders(s.bankSenders);
      setOwn(s.ownAccounts);
      await syncQueue();
    })();
    const sub = AppState.addEventListener('change', state => {
      if (state === 'active') {
        syncQueue();
      }
    });
    return () => sub.remove();
  }, [syncQueue]);

  const importPastSms = async () => {
    await saveSettings({ bankSenders: senders, ownAccounts: own });
    const granted = await requestSmsPermissions();
    if (!granted) {
      setStatus('SMS permission was denied. Enable it in Android settings.');
      return;
    }
    setStatus('Reading inbox...');
    try {
      const raw = await readInbox(2000);
      const r = await ingest(raw);
      setStatus(`Read ${r.found} transaction SMS, added ${r.added} new.`);
      await refresh();
    } catch (e: any) {
      setStatus(`Error: ${e?.message ?? String(e)}`);
    }
  };

  const months = monthlyTotals(rows);
  const categories = expenseByCategory(rows);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.pad}>
        <Text style={styles.h1}>Zelu</Text>

        <Text style={styles.label}>Bank sender IDs (comma separated)</Text>
        <TextInput
          style={styles.input}
          value={senders}
          onChangeText={setSenders}
          placeholder="e.g. HDFCBK, SBIINB, ICICIB"
          autoCapitalize="characters"
        />

        <Text style={styles.label}>Your own UPI IDs or names (comma separated)</Text>
        <TextInput
          style={styles.input}
          value={own}
          onChangeText={setOwn}
          placeholder="e.g. yourname@okaxis"
          autoCapitalize="none"
        />

        <Button title="Import past SMS" onPress={importPastSms} />
        {!!status && <Text style={styles.status}>{status}</Text>}

        <Text style={styles.h2}>Monthly</Text>
        {months.length === 0 && <Text style={styles.line}>No transactions yet.</Text>}
        {months.map(m => (
          <Text key={m.month} style={styles.line}>
            {m.month}: in Rs {m.income.toFixed(2)} | out Rs {m.expense.toFixed(2)} | net Rs{' '}
            {m.net.toFixed(2)}
          </Text>
        ))}

        <Text style={styles.h2}>Expenses by category</Text>
        {categories.map(([cat, total]) => (
          <Text key={cat} style={styles.line}>
            {cat}: Rs {total.toFixed(2)}
          </Text>
        ))}

        <Text style={styles.h2}>Recent</Text>
        {rows.slice(0, 30).map(r => (
          <Text key={r.id} style={styles.line}>
            {r.date} {r.kind} {r.category} Rs {r.amount.toFixed(2)} {r.counterparty ?? ''}
          </Text>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  pad: { padding: 16, paddingBottom: 48 },
  h1: { fontSize: 22, fontWeight: '700', marginBottom: 12 },
  h2: { fontSize: 17, fontWeight: '600', marginTop: 20, marginBottom: 6 },
  label: { fontSize: 13, color: '#555', marginTop: 10 },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    padding: 8,
    marginTop: 4,
    marginBottom: 6,
  },
  status: { marginTop: 8, color: '#333' },
  line: { fontSize: 14, marginBottom: 4 },
});

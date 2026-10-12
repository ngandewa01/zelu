// Bridge to the native Kotlin module (SmsReader) that reads the SMS inbox
// and drains messages captured by the background SMS receiver.
import { NativeModules, PermissionsAndroid, Platform } from 'react-native';

export interface RawSms {
  address: string;
  body: string;
  date: number; // epoch milliseconds
}

const Native = NativeModules.SmsReader;

export async function requestSmsPermissions(): Promise<boolean> {
  if (Platform.OS !== 'android') {
    return false;
  }
  const result = await PermissionsAndroid.requestMultiple([
    PermissionsAndroid.PERMISSIONS.READ_SMS,
    PermissionsAndroid.PERMISSIONS.RECEIVE_SMS,
  ]);
  return Object.values(result).every(
    v => v === PermissionsAndroid.RESULTS.GRANTED,
  );
}

export function readInbox(limit: number = 2000): Promise<RawSms[]> {
  return Native.readInbox(limit);
}

// Messages received while the app was closed, queued by SmsReceiver.
export function drainQueue(): Promise<RawSms[]> {
  return Native.drainQueue();
}

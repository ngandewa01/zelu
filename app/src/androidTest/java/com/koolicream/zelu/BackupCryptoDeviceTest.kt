package com.koolicream.zelu

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// A backup locked with a password opens with that password only, and a damaged or altered file is refused. On a phone,
// because the file format uses Android's Base64 and JSON.
@RunWith(AndroidJUnit4::class)
class BackupCryptoDeviceTest {
    private val plain = """{"app":"zelu","rows":[{"id":"ref-1","amount":349.0,"payee":"SWIGGY ₹"}]}"""

    @Test fun opensWithItsPasswordOnly() {
        val locked = BackupCrypto.encrypt(plain, "correct horse")
        assertTrue(BackupCrypto.isEncrypted(locked))
        assertTrue("the file must not hold the payments in the clear", !locked.contains("SWIGGY"))
        assertEquals(plain, BackupCrypto.decrypt(locked, "correct horse"))
        assertNull(BackupCrypto.decrypt(locked, "correct horsE"))
        // A fresh salt and IV each time: the same backup never makes the same file twice.
        assertNotEquals(locked, BackupCrypto.encrypt(plain, "correct horse"))
    }

    @Test fun alteredFileIsRefused() {
        val o = JSONObject(BackupCrypto.encrypt(plain, "correct horse"))
        val data = o.getString("data")
        val flipped = (if (data[5] == 'A') 'B' else 'A')
        assertNull(BackupCrypto.decrypt(JSONObject(o.toString()).put("data", data.substring(0, 5) + flipped + data.substring(6)).toString(), "correct horse"))
    }

    @Test fun roundCountOutsideTheSafeRangeIsRefused() {
        val o = JSONObject(BackupCrypto.encrypt(plain, "correct horse"))
        // Too few rounds would make guessing cheap, too many would hang the phone: neither file is opened.
        assertNull(BackupCrypto.decrypt(JSONObject(o.toString()).put("iter", 1).toString(), "correct horse"))
        assertNull(BackupCrypto.decrypt(JSONObject(o.toString()).put("iter", Int.MAX_VALUE).toString(), "correct horse"))
    }
}

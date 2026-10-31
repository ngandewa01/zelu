package com.koolicream.zelu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageShapeTest {
    private val kphb1 = "Rs.20000 withdrawn from HDFC Bank Card x1208 at +KPHB-6, 2ND CD on 2024-10-31:17:08:11 Avl bal: 2892252.76.Not You? Call 18002586161/SMS BLOCK DC 1208 to 7308080808"
    private val kphb2 = "Rs.5000 withdrawn from HDFC Bank Card x1208 at +KPHB-6, 2ND CD on 2025-02-11:09:12:40 Avl bal: 152.00.Not You? Call 18002586161/SMS BLOCK DC 1208 to 7308080808"
    private val other = "Rs.5000 withdrawn from HDFC Bank Card x1208 at +HAFEEZPET on 2025-02-11:09:12:40 Avl bal: 152.00.Not You? Call 18002586161"

    @Test fun samePlaceSameShape() = assertEquals(MessageShape.key(kphb1, "AX-HDFCBK-S"), MessageShape.key(kphb2, "VM-HDFCBK-T"))
    @Test fun otherPlaceOtherShape() = assertNotEquals(MessageShape.key(kphb1, "AX-HDFCBK-S"), MessageShape.key(other, "AX-HDFCBK-S"))
    @Test fun otherBankOtherShape() = assertNotEquals(MessageShape.key(kphb1, "AX-HDFCBK-S"), MessageShape.key(kphb1, "AX-ICICIB-S"))
    @Test fun tooShortHasNoShape() = assertNull(MessageShape.key("Rs 500 debited", "AX-HDFCBK-S"))
}

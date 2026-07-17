package dev.pastel.pastelboard.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HidUsageTest {
    @Test
    fun keyboardReportHasStandardEightByteLayout() {
        val report = KeyboardReport(HidUsage.A, setOf(HidModifier.LeftCtrl, HidModifier.LeftShift))

        assertEquals(HidProtocol.KEYBOARD_REPORT_SIZE, report.bytes.size)
        assertArrayEquals(
            byteArrayOf(0x03, 0x00, HidUsage.A.code.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00),
            report.bytes,
        )
    }

    @Test
    fun keyboardReleaseReportIsAllZeroes() {
        assertArrayEquals(ByteArray(HidProtocol.KEYBOARD_REPORT_SIZE), KeyboardReport().bytes)
    }

    @Test
    fun pointerReportHasFourBytesAndClampsFieldsToDescriptorRange() {
        val report = PointerReport(deltaX = 200, deltaY = -200, wheel = 200, buttons = 9)

        assertEquals(HidProtocol.POINTER_REPORT_SIZE, report.bytes.size)
        assertArrayEquals(byteArrayOf(0x07, 0x7F, 0x81.toByte(), 0x7F), report.bytes)
    }

    @Test
    fun descriptorReportIdsMatchSendReportIds() {
        assertEquals(
            setOf(HidProtocol.KEYBOARD_REPORT_ID, HidProtocol.POINTER_REPORT_ID),
            HidProtocol.descriptorReportIds,
        )
    }
}

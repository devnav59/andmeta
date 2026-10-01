package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun read_string_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MT Bubble", appName)
  }

  @Test
  fun test_pip_calculation_rules() {
    // 5-digit broker: 1 Pip = 10 Points
    val eurusd5Digit = Position(
      ticket = 101L,
      symbol = "EURUSD",
      type = 0,
      volume = 0.1,
      openPrice = 1.08500,
      sl = 1.08200,
      tp = 1.09000,
      profit = 15.0,
      digits = 5,
      pointSize = 0.00001
    )
    assertEquals(10.0, eurusd5Digit.pipMultiplier, 0.000001)
    assertEquals(0.00010, eurusd5Digit.pipSize, 0.000001)
    assertTrue(eurusd5Digit.isBuy)

    // 4-digit broker: 1 Pip = 1 Point
    val eurusd4Digit = Position(
      ticket = 102L,
      symbol = "EURUSD",
      type = 1,
      volume = 0.2,
      openPrice = 1.0850,
      sl = 1.0880,
      tp = 1.0800,
      profit = -5.0,
      digits = 4,
      pointSize = 0.0001
    )
    assertEquals(1.0, eurusd4Digit.pipMultiplier, 0.000001)
    assertEquals(0.0001, eurusd4Digit.pipSize, 0.000001)
    assertTrue(eurusd4Digit.isSell)
  }
}

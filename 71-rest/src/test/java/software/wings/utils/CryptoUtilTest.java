package software.wings.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CryptoUtilTest {
  public static final int LEN = 10;

  @Test
  public void testSecureRandAlphaNumString() {
    String alphaNumericPattern = "^[a-zA-Z0-9]*$";

    String randomString1 = CryptoUtil.secureRandAlphaNumString(LEN);
    String randomString2 = CryptoUtil.secureRandAlphaNumString(LEN);

    // test string generated is alphaNumeric
    assertTrue(randomString1.matches(alphaNumericPattern));
    assertTrue(randomString2.matches(alphaNumericPattern));
    // test strings are of expected length and random
    assertEquals(LEN, randomString1.length());
    assertEquals(LEN, randomString2.length());
    assertNotEquals(randomString1, randomString2);

    randomString1 = CryptoUtil.secureRandAlphaNumString(LEN * LEN);
    randomString2 = CryptoUtil.secureRandAlphaNumString(LEN * LEN);

    // test string generated is alphaNumeric
    assertTrue(randomString1.matches(alphaNumericPattern));
    assertTrue(randomString2.matches(alphaNumericPattern));
    // test strings are of expected length and random
    assertEquals(LEN * LEN, randomString1.length());
    assertEquals(LEN * LEN, randomString2.length());
    assertNotEquals(randomString1, randomString2);
  }
}

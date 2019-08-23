package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CryptoUtilsTest extends CategoryTest {
  public static final int LEN = 10;

  @Test
  @Category(UnitTests.class)
  public void testSecureRandAlphaNumString() {
    String alphaNumericPattern = "^[a-zA-Z0-9]*$";

    String randomString1 = CryptoUtils.secureRandAlphaNumString(LEN);
    String randomString2 = CryptoUtils.secureRandAlphaNumString(LEN);

    // test string generated is alphaNumeric
    assertThat(randomString1.matches(alphaNumericPattern)).isTrue();
    assertThat(randomString2.matches(alphaNumericPattern)).isTrue();
    // test strings are of expected length and random
    assertThat(randomString1.length()).isEqualTo(LEN);
    assertThat(randomString2.length()).isEqualTo(LEN);
    assertNotEquals(randomString1, randomString2);

    randomString1 = CryptoUtils.secureRandAlphaNumString(LEN * LEN);
    randomString2 = CryptoUtils.secureRandAlphaNumString(LEN * LEN);

    // test string generated is alphaNumeric
    assertThat(randomString1.matches(alphaNumericPattern)).isTrue();
    assertThat(randomString2.matches(alphaNumericPattern)).isTrue();
    // test strings are of expected length and random
    assertThat(randomString1.length()).isEqualTo(LEN * LEN);
    assertThat(randomString2.length()).isEqualTo(LEN * LEN);
    assertNotEquals(randomString1, randomString2);
  }
}

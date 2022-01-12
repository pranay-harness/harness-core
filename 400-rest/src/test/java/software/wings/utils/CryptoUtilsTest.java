/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CryptoUtilsTest extends CategoryTest {
  public static final int LEN = 10;

  @Test
  @Owner(developers = GEORGE)
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
    assertThat(randomString1).isNotEqualTo(randomString2);

    randomString1 = CryptoUtils.secureRandAlphaNumString(LEN * LEN);
    randomString2 = CryptoUtils.secureRandAlphaNumString(LEN * LEN);

    // test string generated is alphaNumeric
    assertThat(randomString1.matches(alphaNumericPattern)).isTrue();
    assertThat(randomString2.matches(alphaNumericPattern)).isTrue();
    // test strings are of expected length and random
    assertThat(randomString1.length()).isEqualTo(LEN * LEN);
    assertThat(randomString2.length()).isEqualTo(LEN * LEN);
    assertThat(randomString1).isNotEqualTo(randomString2);
  }
}

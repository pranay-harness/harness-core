/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.validator;

import static io.harness.rule.OwnerRule.SAMARTH;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGRegexValidatorConstantsTest extends CategoryTest {
  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testTimeoutPattern() {
    Pattern pattern = Pattern.compile(NGRegexValidatorConstants.TIMEOUT_PATTERN);

    // Valid cases
    assertTrue(pattern.matcher("1m").matches());
    assertTrue(pattern.matcher("1m20s").matches());
    assertTrue(pattern.matcher("1m 20s").matches());

    // Runtime
    assertTrue(pattern.matcher("<+input>").matches());
    assertTrue(pattern.matcher("<+input>.allowedValues()").matches());
    assertTrue(pattern.matcher("<+input>.regex()").matches());
    assertTrue(pattern.matcher("<+2+8>s").matches());
    assertTrue(pattern.matcher("<+random>").matches());

    // Invalid cases
    assertFalse(pattern.matcher("1m  20s").matches());
    assertFalse(pattern.matcher("1m 8").matches());
    assertFalse(pattern.matcher("18").matches());
    assertFalse(pattern.matcher("m").matches());
    assertFalse(pattern.matcher("20mm").matches());
    assertFalse(pattern.matcher("m20m").matches());
    assertFalse(pattern.matcher(" 1m").matches());
    assertFalse(pattern.matcher("1m ").matches());
    assertFalse(pattern.matcher("1 m").matches());
    assertFalse(pattern.matcher("1a").matches());
    assertFalse(pattern.matcher("random").matches());
  }
}

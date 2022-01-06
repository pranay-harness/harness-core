/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.yaml.validation;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class ExpressionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testMatchesPattern() {
    Pattern pattern = Pattern.compile("abc*");
    assertThat(ExpressionUtils.matchesPattern(pattern, null)).isFalse();
    assertThat(ExpressionUtils.matchesPattern(pattern, "a")).isFalse();
    assertThat(ExpressionUtils.matchesPattern(pattern, "abc")).isTrue();
    assertThat(ExpressionUtils.matchesPattern(pattern, "defabc")).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testContainsPattern() {
    Pattern pattern = Pattern.compile("abc*");
    assertThat(ExpressionUtils.containsPattern(pattern, null)).isFalse();
    assertThat(ExpressionUtils.containsPattern(pattern, "a")).isFalse();
    assertThat(ExpressionUtils.containsPattern(pattern, "abc")).isTrue();
    assertThat(ExpressionUtils.containsPattern(pattern, "defabc")).isTrue();
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NGExpressionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMatchesInputSetPattern() {
    String notInputSet = "${inpu}.t.abc";
    String notInputSet1 = "${input.test}";
    String inputSet = "<+input>";
    String inputSet1 = "<+input>.allowedValues(dev, ${env}, ${env2}, stage)";
    String inputSet2 = "<+input>  ";

    assertThat(NGExpressionUtils.matchesInputSetPattern(notInputSet)).isFalse();
    assertThat(NGExpressionUtils.matchesInputSetPattern(notInputSet1)).isFalse();
    assertThat(NGExpressionUtils.matchesInputSetPattern(inputSet)).isTrue();
    assertThat(NGExpressionUtils.matchesInputSetPattern(inputSet1)).isTrue();
    assertThat(NGExpressionUtils.matchesInputSetPattern(inputSet2)).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testContainsPattern() {
    String expression1 = "<+input>.allowedValues(dev, ${env}, ${env2}, stage)";
    String expression2 = "<+input>.allowedValues123(dev, ${env}, ${env2}, stage)";

    String pattern = NGExpressionUtils.getInputSetValidatorPattern("allowedValues");

    boolean contains = NGExpressionUtils.containsPattern(Pattern.compile(pattern), expression1);
    assertThat(contains).isTrue();
    contains = NGExpressionUtils.containsPattern(Pattern.compile(pattern), expression2);
    assertThat(contains).isFalse();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMatchesPattern() {
    String expression = "<+input>.allowedValues(abc)";
    String pattern1 = "<\\+input>.*";
    String pattern2 = "<\\+input>";

    boolean matchesPattern = NGExpressionUtils.matchesPattern(Pattern.compile(pattern1), expression);
    assertThat(matchesPattern).isTrue();
    matchesPattern = NGExpressionUtils.matchesPattern(Pattern.compile(pattern2), expression);
    assertThat(matchesPattern).isFalse();
  }
}

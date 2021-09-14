/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.text.resolver;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class TrackingExpressionResolverTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFindExpressions() {
    List<String> expressions = TrackingExpressionResolver.findExpressions("${", "}", false, false, null);
    assertThat(expressions).isEmpty();

    expressions = TrackingExpressionResolver.findExpressions("${", "}", false, false, "");
    assertThat(expressions).isEmpty();

    expressions = TrackingExpressionResolver.findExpressions(
        "${", "}", false, false, "abc ${def${ghi + ${jkl} + ${mno}}} ${pqr > sty}");
    assertThat(expressions).containsExactly("def${ghi + ${jkl} + ${mno}}", "pqr > sty");

    expressions = TrackingExpressionResolver.findExpressions(
        "${", "}", true, false, "abc ${def${ghi + ${jkl} + ${mno}}} ${pqr > sty}");
    assertThat(expressions).containsExactly("${def${ghi + ${jkl} + ${mno}}}", "${pqr > sty}");

    expressions = TrackingExpressionResolver.findExpressions(
        "${", "}", false, true, "abc ${def${ghi + ${jkl} + ${mno}}} ${pqr > sty}");
    assertThat(expressions).containsExactly("jkl", "mno", "pqr > sty");

    expressions = TrackingExpressionResolver.findExpressions(
        "${", "}", true, true, "abc ${def${ghi + ${jkl} + ${mno}}} ${pqr > sty}");
    assertThat(expressions).containsExactly("${jkl}", "${mno}", "${pqr > sty}");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testIsSingleExpression() {
    assertThat(TrackingExpressionResolver.isSingleExpression("${", "}", null)).isFalse();
    assertThat(TrackingExpressionResolver.isSingleExpression("${", "}", "")).isFalse();
    assertThat(TrackingExpressionResolver.isSingleExpression("${", "}", " ")).isFalse();
    assertThat(TrackingExpressionResolver.isSingleExpression("${", "}", "${abc}")).isTrue();
    assertThat(TrackingExpressionResolver.isSingleExpression("${", "}", "${abc} ")).isFalse();
    assertThat(TrackingExpressionResolver.isSingleExpression("${", "}", "${abc + ${def}}")).isTrue();
  }
}

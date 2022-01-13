/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VariableResolverTrackerTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testTrackVariables() {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    Map<String, Object> context = ImmutableMap.<String, Object>builder()
                                      .put("count", "foo")
                                      .put("direct", "${indirect} ${indirect}")
                                      .put("indirect", "foo")
                                      .build();

    VariableResolverTracker tracker = new VariableResolverTracker();
    expressionEvaluator.substitute("${count} ${dummy} ${direct}", context, tracker);

    assertThat(tracker.getUsage().get("count").get("foo")).isEqualTo(1);
    assertThat(tracker.getUsage().get("indirect").get("foo")).isEqualTo(2);
    assertThat(tracker.getUsage().get("dummy")).isNull();

    expressionEvaluator.substitute("${count} ${dummy}", context, tracker);
    assertThat(tracker.getUsage().get("count").get("foo")).isEqualTo(2);
  }
}

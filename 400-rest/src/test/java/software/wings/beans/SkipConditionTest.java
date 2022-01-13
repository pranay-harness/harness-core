/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.SkipCondition.SkipConditionType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SkipConditionTest extends WingsBaseTest {
  private static final String EXPERSSION_STRING = "${app.name}==\"APP_NAME\"";

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testInstanceForAlwaysSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion("true");
    assertThat(skipCondition.getExpression()).isNull();
    assertThat(skipCondition.getType()).isEqualTo(SkipConditionType.ALWAYS_SKIP);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testInstanceForDoNotSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(null);
    assertThat(skipCondition.getExpression()).isNull();
    assertThat(skipCondition.getType()).isEqualTo(SkipConditionType.DO_NOT_SKIP);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testInstanceForConditionalSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(EXPERSSION_STRING);
    assertThat(skipCondition.getExpression()).isEqualTo(EXPERSSION_STRING);
    assertThat(skipCondition.getType()).isEqualTo(SkipConditionType.CONDITIONAL_SKIP);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFetchDisableAssertionDoNotSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion("true");
    assertThat(skipCondition.fetchDisableAssertion()).isEqualTo("true");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFetchDisableAssertionConditionalSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(null);
    assertThat(skipCondition.fetchDisableAssertion()).isEqualTo(null);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFetchDisableAssertionForAlwaysSkip() {
    SkipCondition skipCondition = SkipCondition.getInstanceForAssertion(EXPERSSION_STRING);
    assertThat(skipCondition.fetchDisableAssertion()).isEqualTo(EXPERSSION_STRING);
  }
}

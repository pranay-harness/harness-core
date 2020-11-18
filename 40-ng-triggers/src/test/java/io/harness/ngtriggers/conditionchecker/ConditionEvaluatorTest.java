package io.harness.ngtriggers.conditionchecker;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConditionEvaluatorTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testEvaluate() {
    assertThat(ConditionEvaluator.evaluate("test", "test", "equals")).isTrue();
    assertThat(ConditionEvaluator.evaluate("test", "test1", "equals")).isFalse();

    assertThat(ConditionEvaluator.evaluate("test", "test1", "not equals")).isTrue();
    assertThat(ConditionEvaluator.evaluate("test", "test", "not equals")).isFalse();

    assertThat(ConditionEvaluator.evaluate("prod_deploy", "prod", "starts with")).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod_deploy", "qa", "starts with")).isFalse();

    assertThat(ConditionEvaluator.evaluate("deploy_prod", "prod", "ends with")).isTrue();
    assertThat(ConditionEvaluator.evaluate("deploy_prod", "qa", "ends with")).isFalse();

    assertThat(ConditionEvaluator.evaluate("prod", "prod, qa, stage", "in")).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod", "qa, stage", "in")).isFalse();
    assertThat(ConditionEvaluator.evaluate("prod,d", "\"prod,d\", qa, stage", "in")).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod,\"d", "\"prod,\"\"d\", qa, stage", "in")).isTrue();

    assertThat(ConditionEvaluator.evaluate("prod", "qa, stage, uat", "not in")).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod", "prod, qa, stage, uat", "not in")).isFalse();
  }
}

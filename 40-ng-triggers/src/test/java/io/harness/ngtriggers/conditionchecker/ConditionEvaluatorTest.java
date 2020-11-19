package io.harness.ngtriggers.conditionchecker;

import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.ENDS_WITH_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.EQUALS_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.IN_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.NOT_EQUALS_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.NOT_IN_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.STARTS_WITH_OPERATOR;
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
    assertThat(ConditionEvaluator.evaluate("test", "test", EQUALS_OPERATOR)).isTrue();
    assertThat(ConditionEvaluator.evaluate("test", "test1", EQUALS_OPERATOR)).isFalse();

    assertThat(ConditionEvaluator.evaluate("test", "test1", NOT_EQUALS_OPERATOR)).isTrue();
    assertThat(ConditionEvaluator.evaluate("test", "test", NOT_EQUALS_OPERATOR)).isFalse();

    assertThat(ConditionEvaluator.evaluate("prod_deploy", "prod", STARTS_WITH_OPERATOR)).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod_deploy", "qa", STARTS_WITH_OPERATOR)).isFalse();

    assertThat(ConditionEvaluator.evaluate("deploy_prod", "prod", ENDS_WITH_OPERATOR)).isTrue();
    assertThat(ConditionEvaluator.evaluate("deploy_prod", "qa", ENDS_WITH_OPERATOR)).isFalse();

    assertThat(ConditionEvaluator.evaluate("prod", "prod, qa, stage", IN_OPERATOR)).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod", "qa, stage", IN_OPERATOR)).isFalse();
    assertThat(ConditionEvaluator.evaluate("prod,d", "\"prod,d\", qa, stage", IN_OPERATOR)).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod,\"d", "\"prod,\"\"d\", qa, stage", IN_OPERATOR)).isTrue();

    assertThat(ConditionEvaluator.evaluate("prod", "qa, stage, uat", NOT_IN_OPERATOR)).isTrue();
    assertThat(ConditionEvaluator.evaluate("prod", "prod, qa, stage, uat", NOT_IN_OPERATOR)).isFalse();
  }
}

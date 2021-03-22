package io.harness.engine.utils;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.run.NodeRunCheck;
import io.harness.engine.skip.SkipCheck;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class OrchestrationUtilsTest extends OrchestrationTestBase {
  @Mock EngineExpressionService engineExpressionService;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testShouldRunExecution() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    String whenCondition = "WHENCONDITION";
    doReturn("true").when(engineExpressionService).evaluateExpression(ambiance, whenCondition);
    NodeRunCheck nodeRunCheck = OrchestrationUtils.shouldRunExecution(ambiance, whenCondition, engineExpressionService);
    assertThat(nodeRunCheck.isSuccessful()).isTrue();
    assertThat(nodeRunCheck.getEvaluatedWhenCondition()).isTrue();
    assertThat(nodeRunCheck.getWhenCondition()).isEqualTo(whenCondition);

    doThrow(new InvalidArgumentsException("Cannot find expression"))
        .when(engineExpressionService)
        .evaluateExpression(ambiance, whenCondition);
    nodeRunCheck = OrchestrationUtils.shouldRunExecution(ambiance, whenCondition, engineExpressionService);
    assertThat(nodeRunCheck.isSuccessful()).isFalse();
    assertThat(nodeRunCheck.getWhenCondition()).isEqualTo(whenCondition);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldSkipNodeExecution() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    String skipCondition = "SKIPCONDITION";
    doReturn("true").when(engineExpressionService).evaluateExpression(ambiance, skipCondition);
    SkipCheck skipCheck = OrchestrationUtils.shouldSkipNodeExecution(ambiance, skipCondition, engineExpressionService);
    assertThat(skipCheck.isSuccessful()).isTrue();
    assertThat(skipCheck.getEvaluatedSkipCondition()).isTrue();
    assertThat(skipCheck.getSkipCondition()).isEqualTo(skipCondition);

    doThrow(new InvalidArgumentsException("Cannot find expression"))
        .when(engineExpressionService)
        .evaluateExpression(ambiance, skipCondition);
    skipCheck = OrchestrationUtils.shouldSkipNodeExecution(ambiance, skipCondition, engineExpressionService);
    assertThat(skipCheck.isSuccessful()).isFalse();
    assertThat(skipCheck.getSkipCondition()).isEqualTo(skipCondition);
  }
}
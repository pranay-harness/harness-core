package io.harness.engine.facilitation;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.interrupts.PreFacilitationCheck;
import io.harness.execution.NodeExecution;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AbstractPreFacilitationCheckerTest extends OrchestrationTestBase {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCheck() {
    InterruptPreFacilitationChecker iChecker = spy(InterruptPreFacilitationChecker.class);
    RunPreFacilitationChecker rChecker = spy(RunPreFacilitationChecker.class);
    SkipPreFacilitationChecker sChecker = spy(SkipPreFacilitationChecker.class);
    iChecker.setNextChecker(rChecker);
    rChecker.setNextChecker(sChecker);

    doReturn(PreFacilitationCheck.builder().proceed(true).build())
        .when(iChecker)
        .performCheck(any(NodeExecution.class));
    doReturn(PreFacilitationCheck.builder().proceed(true).build())
        .when(rChecker)
        .performCheck(any(NodeExecution.class));
    doReturn(PreFacilitationCheck.builder().proceed(true).build())
        .when(sChecker)
        .performCheck(any(NodeExecution.class));

    iChecker.check(any(NodeExecution.class));

    verify(iChecker).performCheck(any(NodeExecution.class));
    verify(rChecker).performCheck(any(NodeExecution.class));
    verify(sChecker).performCheck(any(NodeExecution.class));
  }
}
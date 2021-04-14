package io.harness.decorators;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class ExecutableResponseDecoratorTest extends OrchestrationTestBase {
  private static final String TASK_ID = "taskId";
  private ExecutableResponseDecorator executableResponseDecorator;

  @Before
  public void setup() {
    executableResponseDecorator = ExecutableResponseDecorator.builder().taskId(TASK_ID).build();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testDecorate() {
    ExecutableResponse executableResponse =
        ExecutableResponse.newBuilder()
            .setTaskChain(TaskChainExecutableResponse.newBuilder().setTaskCategory(TaskCategory.DELEGATE_TASK_V2))
            .build();
    assertThat(executableResponseDecorator.decorate(executableResponse))
        .isEqualTo(ExecutableResponse.newBuilder()
                       .setTaskChain(TaskChainExecutableResponse.newBuilder().setTaskId(TASK_ID).setTaskCategory(
                           TaskCategory.DELEGATE_TASK_V2))
                       .build());
  }
}
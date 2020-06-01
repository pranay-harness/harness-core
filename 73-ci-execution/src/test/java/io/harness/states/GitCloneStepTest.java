package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.execution.status.Status;
import io.harness.executionplan.CIExecutionTest;
import io.harness.managerclient.ManagerCIResource;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class GitCloneStepTest extends CIExecutionTest {
  @Inject private GitCloneStep gitCloneStep;
  @Mock private ManagerCIResource managerCIResource;

  @Before
  public void setUp() {
    on(gitCloneStep).set("managerCIResource", managerCIResource);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldExecuteGitCloneTask() {
    StepResponse stepResponse = gitCloneStep.executeSync(null, GitCloneStepInfo.builder().build(), null, null);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }
}
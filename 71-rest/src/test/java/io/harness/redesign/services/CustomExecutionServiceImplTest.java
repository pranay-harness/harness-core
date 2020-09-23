package io.harness.redesign.services;

import static io.harness.execution.status.Status.RUNNING;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.beans.Graph;
import io.harness.beans.GraphVertex;
import io.harness.category.element.UnitTests;
import io.harness.config.MockServerConfig;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.plan.Plan;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.steps.dummy.DummyStep;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.events.TestUtils;
import software.wings.security.UserThreadLocal;

/**
 * Test class for {@link CustomExecutionServiceImpl}
 */
public class CustomExecutionServiceImplTest extends WingsBaseTest {
  @Inject private TestUtils testUtils;
  @Mock private OrchestrationService orchestrationService;
  @Mock private GraphGenerationService graphGenerationService;
  @InjectMocks @Inject private CustomExecutionServiceImpl customExecutionService;

  @Mock private MainConfiguration configuration;
  @InjectMocks @Inject CustomExecutionProvider customExecutionProvider;

  private User user;

  @Before
  public void setup() {
    Account account = testUtils.createAccount();
    user = testUtils.createUser(account);

    when(configuration.getMockServerConfig()).thenReturn(new MockServerConfig());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldExecuteHttpSwitch() {
    UserThreadLocal.set(user);
    Plan expectedSwitchHttpPlan = customExecutionProvider.provideHttpSwitchPlan();
    when(orchestrationService.startExecution(any(), any()))
        .thenReturn(PlanExecution.builder().status(RUNNING).plan(expectedSwitchHttpPlan).build());
    PlanExecution planExecutionResponse = customExecutionService.executeHttpSwitch();

    assertThat(planExecutionResponse.getPlan()).isEqualTo(expectedSwitchHttpPlan);
    assertThat(planExecutionResponse.getStatus()).isEqualTo(RUNNING);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldExecuteHttpFork() {
    UserThreadLocal.set(user);
    Plan expectedForkPlan = customExecutionProvider.provideHttpForkPlan();
    when(orchestrationService.startExecution(any(), any()))
        .thenReturn(PlanExecution.builder().status(RUNNING).plan(expectedForkPlan).build());
    PlanExecution planExecutionResponse = customExecutionService.executeHttpFork();

    assertThat(planExecutionResponse.getPlan()).isEqualTo(expectedForkPlan);
    assertThat(planExecutionResponse.getStatus()).isEqualTo(RUNNING);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldExecuteSectionPlan() {
    UserThreadLocal.set(user);
    Plan expectedSelectionPlan = customExecutionProvider.provideHttpSectionPlan();
    when(orchestrationService.startExecution(any(), any()))
        .thenReturn(PlanExecution.builder().status(RUNNING).plan(expectedSelectionPlan).build());
    PlanExecution planExecutionResponse = customExecutionService.executeSectionPlan();

    assertThat(planExecutionResponse.getPlan()).isEqualTo(expectedSelectionPlan);
    assertThat(planExecutionResponse.getStatus()).isEqualTo(RUNNING);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldExecuteRetryPlan() {
    UserThreadLocal.set(user);
    Plan expectedRetryPlan = customExecutionProvider.provideHttpRetryIgnorePlan();
    when(orchestrationService.startExecution(any(), any()))
        .thenReturn(PlanExecution.builder().status(RUNNING).plan(expectedRetryPlan).build());
    PlanExecution planExecutionResponse = customExecutionService.executeRetryIgnorePlan();

    assertThat(planExecutionResponse.getPlan()).isEqualTo(expectedRetryPlan);
    assertThat(planExecutionResponse.getStatus()).isEqualTo(RUNNING);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldExecuteSimpleShellScriptPlan() {
    UserThreadLocal.set(user);
    Plan expectedShellScriptPlan = customExecutionProvider.provideSimpleShellScriptPlan();
    when(orchestrationService.startExecution(any(), any()))
        .thenReturn(PlanExecution.builder().status(Status.RUNNING).plan(expectedShellScriptPlan).build());
    PlanExecution planExecutionResponse = customExecutionService.executeSimpleShellScriptPlan(ACCOUNT_ID, APP_ID);

    assertThat(planExecutionResponse.getPlan()).isEqualTo(expectedShellScriptPlan);
    assertThat(planExecutionResponse.getStatus()).isEqualTo(Status.RUNNING);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldReturnGraph() {
    String planExecutionId = "planExecutionId";
    GraphVertex graphVertex = GraphVertex.builder()
                                  .uuid("id")
                                  .name("node1")
                                  .stepType(DummyStep.STEP_TYPE.getType())
                                  .next(null)
                                  .subgraph(null)
                                  .build();
    when(graphGenerationService.generateGraph(planExecutionId))
        .thenReturn(Graph.builder().graphVertex(graphVertex).build());
    Graph graph = customExecutionService.getGraph(planExecutionId);

    assertThat(graph).isNotNull();
    assertThat(graph.getGraphVertex().getUuid()).isEqualTo(graphVertex.getUuid());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldExecuteMultipleBarriersPlan() {
    UserThreadLocal.set(user);
    Plan expectedMultipleBarriersPlan = customExecutionProvider.providePlanWithMultipleBarriers();
    when(orchestrationService.startExecution(any(), any()))
        .thenReturn(PlanExecution.builder().status(Status.RUNNING).plan(expectedMultipleBarriersPlan).build());
    PlanExecution planExecutionResponse = customExecutionService.executeMultipleBarriersPlan();

    assertThat(planExecutionResponse.getPlan()).isEqualTo(expectedMultipleBarriersPlan);
    assertThat(planExecutionResponse.getStatus()).isEqualTo(Status.RUNNING);
  }
}

package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RepairActionCode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipelineServiceDBTest extends WingsBaseTest {
  @Mock private JobScheduler jobScheduler;

  @Inject @InjectMocks private AccountService accountService;
  @Inject @InjectMocks private AppService appService;
  @Inject @InjectMocks private WorkflowService workflowService;
  @Inject @InjectMocks private PipelineService pipelineService;
  @Inject @InjectMocks private WingsPersistence wingsPersistence;

  Account createAccount() {
    Account account = anAccount().withAccountName("test-account").withCompanyName("Harness").build();
    return accountService.save(account);
  }

  private Application createApplication(Account account) {
    Application application = anApplication().withName("test-application").withAccountId(account.getUuid()).build();
    return appService.save(application);
  }

  private Workflow createWorkflow(Application application) {
    Graph graph = aGraph()
                      .addNodes(aGraphNode()
                                    .withId("n1")
                                    .withName("stop")
                                    .withType(StateType.ENV_STATE.name())
                                    .withOrigin(true)
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
                      .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
                      .build();

    BuildWorkflow orchestrationWorkflow = aBuildOrchestrationWorkflow().withGraph(graph).build();
    Workflow workflow = aWorkflow()
                            .withAppId(application.getUuid())
                            .withName("workflow1")
                            .withWorkflowType(WorkflowType.ORCHESTRATION)
                            .withOrchestrationWorkflow(orchestrationWorkflow)
                            .build();

    return workflowService.createWorkflow(workflow);
  }

  @Test
  public void shouldUpdatePipeline() {
    final Account account = createAccount();
    final Application application = createApplication(account);
    final Workflow workflow = createWorkflow(application);

    Map<String, Object> properties = new HashMap<>();
    properties.put("workflowId", workflow.getUuid());

    PipelineStage pipelineStage =
        PipelineStage.builder()
            .pipelineStageElements(asList(
                PipelineStageElement.builder().name("STAGE1").type(ENV_STATE.name()).properties(properties).build()))
            .build();

    Pipeline pipeline = Pipeline.builder()
                            .name("pipeline1")
                            .appId(application.getUuid())
                            .uuid(PIPELINE_ID)
                            .pipelineStages(asList(pipelineStage))
                            .build();

    pipelineService.save(pipeline);

    List<FailureStrategy> failureStrategies =
        asList(FailureStrategy.builder().repairActionCode(RepairActionCode.MANUAL_INTERVENTION).build());

    pipelineService.updateFailureStrategies(application.getUuid(), pipeline.getUuid(), failureStrategies);

    Pipeline updated = wingsPersistence.get(Pipeline.class, application.getUuid(), pipeline.getUuid());

    assertThat(updated.getFailureStrategies()).isEqualTo(failureStrategies);
  }
}

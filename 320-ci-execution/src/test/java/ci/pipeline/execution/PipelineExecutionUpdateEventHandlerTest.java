package ci.pipeline.execution;

import static io.harness.rule.OwnerRule.HARSH;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.utils.Lists;
import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.execution.NodeExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.plan.PlanNode;
import io.harness.plancreators.IntegrationStagePlanCreator;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.Status;
import io.harness.rule.Owner;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PipelineExecutionUpdateEventHandlerTest extends CategoryTest {
  @Mock private NodeExecutionServiceImpl nodeExecutionService;
  @Mock private GitBuildStatusUtility gitBuildStatusUtility;
  @InjectMocks private PipelineExecutionUpdateEventHandler pipelineExecutionUpdateEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.builder()
                          .setupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier", "projectIdentfier",
                              "orgIdentifier", "orgIdentifier"))
                          .levels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .build())
            .build();
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .status(Status.RUNNING)
                                      .resolvedStepParameters(IntegrationStageStepParameters.builder()
                                                                  .buildStatusUpdateParameter(null)
                                                                  .integrationStage(null)
                                                                  .build())
                                      .node(PlanNode.builder().group(IntegrationStagePlanCreator.GROUP_NAME).build())
                                      .build();
    when(gitBuildStatusUtility.shouldSendStatus(any())).thenReturn(true);
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(gitBuildStatusUtility).sendStatusToGit(any(), any(), any());
  }
}

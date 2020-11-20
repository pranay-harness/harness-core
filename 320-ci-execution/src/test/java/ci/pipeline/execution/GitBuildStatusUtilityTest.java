package ci.pipeline.execution;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.category.element.UnitTests;
import io.harness.execution.NodeExecution;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.git.GitClientHelper;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plan.PlanNode;
import io.harness.plancreators.IntegrationStagePlanCreator;
import io.harness.pms.execution.Status;
import io.harness.rule.Owner;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.stateutils.buildstate.ConnectorUtils;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

public class GitBuildStatusUtilityTest extends CIExecutionTest {
  private final String accountId = "accountId";
  @Mock private GitClientHelper gitClientHelper;
  @Mock private ConnectorUtils connectorUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @InjectMocks private GitBuildStatusUtility gitBuildStatusUtility;
  private Ambiance ambiance = Ambiance.builder()
                                  .setupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                                      "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                                  .build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testHandleEventForRunning() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.RUNNING);

    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());

    gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution)).isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testHandleEventForSuccess() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.SUCCEEDED);
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());

    gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution)).isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testHandleEventForError() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ERRORED);
    gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution)).isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testHandleEventForAborted() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ABORTED);
    gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution)).isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testHandleEventForUNSUPPORTED() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.QUEUED);
    gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution)).isEqualTo(true);
    verify(delegateGrpcClientWrapper, never()).submitAsyncTask(any());
  }

  private NodeExecution getNodeExecution(Status status) {
    return NodeExecution.builder()
        .status(status)
        .resolvedStepParameters(IntegrationStageStepParameters.builder()
                                    .buildStatusUpdateParameter(BuildStatusUpdateParameter.builder()
                                                                    .state("error")
                                                                    .sha("sha")
                                                                    .identifier("identifier")
                                                                    .desc("desc")
                                                                    .build())
                                    .integrationStage(null)
                                    .build())
        .node(PlanNode.builder().group(IntegrationStagePlanCreator.GROUP_NAME).build())
        .build();
  }
}

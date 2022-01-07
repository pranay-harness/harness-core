/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package ci.pipeline.execution;

import static io.harness.rule.OwnerRule.HARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.category.element.UnitTests;
import io.harness.execution.NodeExecution;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.git.GitClientHelper;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.rule.Owner;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import java.io.IOException;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitBuildStatusUtilityTest extends CIExecutionTestBase {
  private final String accountId = "accountId";
  @Mock private GitClientHelper gitClientHelper;
  @Mock private ConnectorUtils connectorUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @InjectMocks private GitBuildStatusUtility gitBuildStatusUtility;
  private Ambiance ambiance = Ambiance.newBuilder()
                                  .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                                      "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                                  .build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testHandleEventForRunning() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.RUNNING);

    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());

    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testRunningHandleEventForBitBucket() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.RUNNING);

    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitLabConnector());

    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testRunningHandleEventForGitlab() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.RUNNING);

    when(connectorUtils.getConnectorDetails(any(), any()))
        .thenReturn(ciExecutionPlanTestHelper.getBitBucketConnector());

    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testHandleEventForSuccess() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.SUCCEEDED);
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());

    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testGitlabHandleEventForSuccess() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.SUCCEEDED);
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitLabConnector());

    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testBitbucketHandleEventForSuccess() throws IOException {
    NodeExecution nodeExecution = getNodeExecution(Status.SUCCEEDED);
    when(connectorUtils.getConnectorDetails(any(), any()))
        .thenReturn(ciExecutionPlanTestHelper.getBitBucketConnector());

    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testHandleEventForError() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ERRORED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testGitlabHandleEventForError() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitLabConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ERRORED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testBitbucketHandleEventForError() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any()))
        .thenReturn(ciExecutionPlanTestHelper.getBitBucketConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ERRORED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testHandleEventForAborted() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ABORTED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testGitlabHandleEventForAborted() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitLabConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ABORTED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testBitbucketHandleEventForAborted() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any()))
        .thenReturn(ciExecutionPlanTestHelper.getBitBucketConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.ABORTED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testHandleEventForUNSUPPORTED() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.QUEUED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper, never()).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testGitlabHandleEventForUNSUPPORTED() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitLabConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.QUEUED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper, never()).submitAsyncTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testBitbucketHandleEventForUNSUPPORTED() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any()))
        .thenReturn(ciExecutionPlanTestHelper.getBitBucketConnector());
    NodeExecution nodeExecution = getNodeExecution(Status.QUEUED);
    gitBuildStatusUtility.sendStatusToGit(
        nodeExecution.getStatus(), getNodeExecutionStepParameters(), ambiance, accountId);
    assertThat(gitBuildStatusUtility.shouldSendStatus(nodeExecution.getNode().getStepType().getStepCategory()))
        .isEqualTo(true);
    verify(delegateGrpcClientWrapper, never()).submitAsyncTask(any(), any());
  }

  private NodeExecution getNodeExecution(Status status) {
    return NodeExecution.builder()
        .status(status)
        .resolvedStepParameters(getNodeExecutionStepParameters())
        .node(PlanNodeProto.newBuilder()
                  .setStepType(StepType.newBuilder().setStepCategory(StepCategory.STAGE).setType("IntegrationStage"))
                  .build())
        .build();
  }

  private StepParameters getNodeExecutionStepParameters() {
    return IntegrationStageStepParametersPMS.builder()
        .buildStatusUpdateParameter(BuildStatusUpdateParameter.builder()
                                        .state("error")
                                        .sha("sha")
                                        .identifier("identifier")
                                        .desc("desc")
                                        .build())
        .build();
  }
}

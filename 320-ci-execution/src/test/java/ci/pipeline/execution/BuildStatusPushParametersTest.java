/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package ci.pipeline.execution;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.delegate.beans.connector.ConnectorType.GITHUB;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.REPO;
import static io.harness.rule.OwnerRule.JAMIE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.PipelineUtils;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.task.ci.CIBuildStatusPushParameters;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.ConnectorUtils;

import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CI)
public class BuildStatusPushParametersTest extends CIExecutionTestBase {
  private static final String SOME_URL = "https://url.com/owner/repo.git";

  @Mock private ConnectorUtils connectorUtils;
  @Mock GithubConnectorDTO gitConfigDTO;
  @Mock private PipelineUtils pipelineUtils;
  @Mock private ConnectorDetails connectorDetails;
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
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testIdentifierGeneration() {
    prepareRepoLevelConnector(SOME_URL);
    ExecutionMetadata executionMetadata = ExecutionMetadata.newBuilder()
                                              .setExecutionUuid("executionuuid")
                                              .setPipelineIdentifier("shortPipelineId")
                                              .build();
    BuildStatusUpdateParameter buildStatusUpdateParameter =
        getBuildStatusUpdateParameter("shortIdentifier", "shortname");

    CIBuildStatusPushParameters pushParameters = gitBuildStatusUtility.getCIBuildStatusPushParams(
        Ambiance.newBuilder(ambiance).setMetadata(executionMetadata).build(), buildStatusUpdateParameter,
        Status.SUCCEEDED, "sha");

    assertThat(pushParameters.getDesc())
        .isEqualTo("Execution status of Pipeline - shortPipelineId (executionuuid) Stage - shortname was SUCCEEDED");

    assertThat(pushParameters.getIdentifier()).isEqualTo("shortPipelineId-shortIdentifier");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testRepoNameGeneration() {
    prepareAccountLevelConnector("https://github.com/");
    ExecutionMetadata executionMetadata =
        ExecutionMetadata.newBuilder().setExecutionUuid("executionuuid").setPipelineIdentifier("pipelineId").build();
    BuildStatusUpdateParameter buildStatusUpdateParameter =
        getBuildStatusUpdateParameter("shortIdentifier", "shortname", "wings-software/jhttp");

    CIBuildStatusPushParameters pushParameters = gitBuildStatusUtility.getCIBuildStatusPushParams(
        Ambiance.newBuilder(ambiance).setMetadata(executionMetadata).build(), buildStatusUpdateParameter,
        Status.SUCCEEDED, "sha");

    assertThat(pushParameters.getDesc())
        .isEqualTo("Execution status of Pipeline - pipelineId (executionuuid) Stage - shortname was SUCCEEDED");

    assertThat(pushParameters.getIdentifier()).isEqualTo("pipelineId-shortIdentifier");
    assertThat(pushParameters.getOwner()).isEqualTo("wings-software");
    assertThat(pushParameters.getRepo()).isEqualTo("jhttp");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testIdentifierGenerationLongName() {
    prepareRepoLevelConnector(SOME_URL);
    ExecutionMetadata executionMetadata =
        ExecutionMetadata.newBuilder()
            .setExecutionUuid("executionuuid")
            .setPipelineIdentifier(
                "longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongPipline")
            .build();
    BuildStatusUpdateParameter buildStatusUpdateParameter = getBuildStatusUpdateParameter(
        "longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongId",
        "longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongName");

    CIBuildStatusPushParameters pushParameters = gitBuildStatusUtility.getCIBuildStatusPushParams(
        Ambiance.newBuilder(ambiance).setMetadata(executionMetadata).build(), buildStatusUpdateParameter,
        Status.SUCCEEDED, "sha");

    assertThat(pushParameters.getDesc())
        .isEqualTo(
            "Execution status of Pipeline - longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongPipline (executionuuid) Stage - longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongName was SUCCEEDED");

    assertThat(pushParameters.getIdentifier())
        .isEqualTo("longlonglonglonglonglonglon...-longlonglonglonglonglonglon...");
  }

  private BuildStatusUpdateParameter getBuildStatusUpdateParameter(String identifier, String name) {
    return BuildStatusUpdateParameter.builder().identifier(identifier).buildNumber("0").desc("desc").name(name).build();
  }

  private BuildStatusUpdateParameter getBuildStatusUpdateParameter(String identifier, String name, String repo) {
    return BuildStatusUpdateParameter.builder()
        .connectorIdentifier("testConnector")
        .repoName(repo)
        .identifier(identifier)
        .buildNumber("0")
        .desc("desc")
        .name(name)
        .build();
  }

  private void prepareAccountLevelConnector(String url) {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(connectorDetails);
    when(connectorDetails.getConnectorType()).thenReturn(GITHUB);
    when(connectorDetails.getConnectorConfig()).thenReturn(gitConfigDTO);
    when(gitConfigDTO.getUrl()).thenReturn(url);
    when(gitConfigDTO.getConnectionType()).thenReturn(ACCOUNT);
    when(pipelineUtils.getBuildDetailsUrl(any(), any(), any(), any())).thenReturn(SOME_URL);
  }

  private void prepareRepoLevelConnector(String url) {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(connectorDetails);
    when(connectorDetails.getConnectorType()).thenReturn(GITHUB);
    when(connectorDetails.getConnectorConfig()).thenReturn(gitConfigDTO);
    when(gitConfigDTO.getUrl()).thenReturn(url);
    when(gitConfigDTO.getConnectionType()).thenReturn(REPO);
    when(pipelineUtils.getBuildDetailsUrl(any(), any(), any(), any())).thenReturn(SOME_URL);
  }
}

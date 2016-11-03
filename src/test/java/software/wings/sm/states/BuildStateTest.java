package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.BuildStateExecutionData;
import software.wings.service.intfc.ArtifactService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;

/**
 * Created by anubhaw on 11/3/16.
 */
public class BuildStateTest extends WingsBaseTest {
  @Mock private ArtifactService artifactService;
  @Mock private ExecutionContextImpl context;

  @InjectMocks private BuildState buildState = new BuildState("BUILD_STATE");

  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS = aWorkflowStandardParams().build();

  @Before
  public void setUp() throws Exception {
    buildState.setArtifactStreamId(ARTIFACT_STREAM_ID);
    when(context.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(artifactService.fetchLatestArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID))
        .thenReturn(anArtifact().withUuid(ARTIFACT_ID).withDisplayName(ARTIFACT_NAME).build());
  }

  @Test
  public void shouldExecute() {
    assertThat(WORKFLOW_STANDARD_PARAMS.getArtifactIds()).isNull();
    ExecutionResponse executionResponse = buildState.execute(context);
    assertThat(WORKFLOW_STANDARD_PARAMS.getArtifactIds()).hasSize(1).containsExactly(ARTIFACT_ID);
    assertThat(executionResponse.isAsync()).isFalse();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    BuildStateExecutionData stateExecutionData = (BuildStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getArtifactId()).isEqualTo(ARTIFACT_ID);
    assertThat(stateExecutionData.getArtifactName()).isEqualTo(ARTIFACT_NAME);
    assertThat(stateExecutionData.getArtifactStreamId()).isEqualTo(ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldFailExecutionIfNoArtifactFound() {
    when(artifactService.fetchLatestArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(null);
    ExecutionResponse executionResponse = buildState.execute(context);
    assertThat(executionResponse.isAsync()).isFalse();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }
}

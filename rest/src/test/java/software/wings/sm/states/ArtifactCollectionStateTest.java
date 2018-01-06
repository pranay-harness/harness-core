package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

/**
 * Created by sgurubelli on 11/28/17.
 */
public class ArtifactCollectionStateTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private ExecutionContextImpl executionContext;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private JobScheduler jobScheduler;
  @Mock private ArtifactService artifactService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @InjectMocks
  private ArtifactCollectionState artifactCollectionState = new ArtifactCollectionState("Collect Artifact");

  private JenkinsArtifactStream jenkinsArtifactStream = aJenkinsArtifactStream()
                                                            .withAppId(APP_ID)
                                                            .withUuid(ARTIFACT_STREAM_ID)
                                                            .withSourceName(ARTIFACT_SOURCE_NAME)
                                                            .withSettingId(SETTING_ID)
                                                            .withJobname("JOB")
                                                            .withServiceId(SERVICE_ID)
                                                            .withArtifactPaths(asList("*WAR"))
                                                            .build();

  @Before
  public void setUp() throws Exception {
    artifactCollectionState.setArtifactStreamId(ARTIFACT_STREAM_ID);
    when(executionContext.getAppId()).thenReturn(APP_ID);
    when(executionContext.getApp()).thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build());
    when(executionContext.getEnv()).thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLatestArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
  }

  @Test
  public void shouldExecute() throws Exception {
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull().hasFieldOrPropertyWithValue("async", true);
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
  }

  @Test
  public void shouldHandleAsyncResponse() throws Exception {
    when(executionContext.getStateExecutionData()).thenReturn(ArtifactCollectionExecutionData.builder().build());
    artifactCollectionState.handleAsyncResponse(executionContext,
        ImmutableMap.of(
            ACTIVITY_ID, ArtifactCollectionExecutionData.builder().artifactStreamId(ARTIFACT_STREAM_ID).build()));
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), anyString(), any());
  }
}

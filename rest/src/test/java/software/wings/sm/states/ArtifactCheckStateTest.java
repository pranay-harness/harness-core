package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelayEventHelper;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.CronUtil;

import java.util.UUID;

/**
 * Created by rsingh on 5/4/18.
 */
public class ArtifactCheckStateTest extends WingsBaseTest {
  @Inject private ArtifactService artifactService;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private CronUtil cronUtil;
  @Mock private ExecutionContext context;
  @Mock FeatureFlagService featureFlagService;
  @Mock DelayEventHelper delayEventHelper;

  private String accountId;
  private String appId;
  private WorkflowStandardParams workflowStandardParams;
  private ArtifactCheckState artifactCheckState = new ArtifactCheckState("ArtifactCheckState");

  @Before
  public void setUp() {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    workflowStandardParams = aWorkflowStandardParams().withAppId(appId).build();
    setInternalState(workflowStandardParams, "artifactService", artifactService);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(context.getAppId()).thenReturn(appId);
    when(cronUtil.scheduleReminder(anyLong(), anyString(), anyString())).thenReturn(UUID.randomUUID().toString());
    when(delayEventHelper.delay(anyInt(), any())).thenReturn("anyGUID");
    setInternalState(artifactCheckState, "artifactService", artifactService);
    setInternalState(artifactCheckState, "cronUtil", cronUtil);
    setInternalState(artifactCheckState, "delayEventHelper", delayEventHelper);
    setInternalState(artifactCheckState, "featureFlagService", featureFlagService);
  }

  @Test
  public void emptyArtifacts() {
    ExecutionResponse executionResponse = artifactCheckState.execute(context);
    assertEquals("Artifacts are not required.", executionResponse.getErrorMessage());
  }

  @Test
  public void failedArtifacts() {
    String failedArtifactId = wingsPersistence.save(anArtifact().withStatus(Status.FAILED).withAppId(appId).build());
    String errorArtifactId = wingsPersistence.save(anArtifact().withStatus(Status.ERROR).withAppId(appId).build());
    workflowStandardParams.setArtifactIds(Lists.newArrayList(failedArtifactId, errorArtifactId));

    ExecutionResponse executionResponse = artifactCheckState.execute(context);
    assertEquals(ExecutionStatus.FAILED, executionResponse.getExecutionStatus());
  }

  @Test
  public void allDownloadedArtifacts() {
    String artifactId1 = wingsPersistence.save(anArtifact()
                                                   .withStatus(Status.READY)
                                                   .withAppId(appId)
                                                   .withContentStatus(ContentStatus.DOWNLOADED)
                                                   .withDisplayName("artifact1")
                                                   .build());
    String artifactId2 = wingsPersistence.save(anArtifact()
                                                   .withStatus(Status.READY)
                                                   .withAppId(appId)
                                                   .withContentStatus(ContentStatus.DOWNLOADED)
                                                   .withDisplayName("artifact2")
                                                   .build());
    workflowStandardParams.setArtifactIds(Lists.newArrayList(artifactId1, artifactId2));

    ExecutionResponse executionResponse = artifactCheckState.execute(context);
    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    assertEquals("All artifacts: [artifact1, artifact2] are available.", executionResponse.getErrorMessage());
  }

  @Test
  public void kickDownloadArtifacts() {
    ArtifactStream artifactStream1 = new JenkinsArtifactStream();
    artifactStream1.setAppId(appId);
    String artifactStreamId1 = wingsPersistence.save(artifactStream1);

    String artifactId1 = wingsPersistence.save(anArtifact()
                                                   .withStatus(Status.READY)
                                                   .withAppId(appId)
                                                   .withContentStatus(ContentStatus.NOT_DOWNLOADED)
                                                   .withDisplayName("artifact1")
                                                   .withArtifactStreamId(artifactStreamId1)
                                                   .build());

    ArtifactStream artifactStream2 = new JenkinsArtifactStream();
    artifactStream2.setAppId(appId);
    String artifactStreamId2 = wingsPersistence.save(artifactStream2);

    String artifactId2 = wingsPersistence.save(anArtifact()
                                                   .withStatus(Status.READY)
                                                   .withAppId(appId)
                                                   .withContentStatus(ContentStatus.NOT_DOWNLOADED)
                                                   .withDisplayName("artifact2")
                                                   .withArtifactStreamId(artifactStreamId2)
                                                   .build());
    workflowStandardParams.setArtifactIds(Lists.newArrayList(artifactId1, artifactId2));

    ExecutionResponse executionResponse = artifactCheckState.execute(context);
    assertEquals(ExecutionStatus.SUCCESS, executionResponse.getExecutionStatus());
    assertEquals("Waiting for artifacts:[artifact1, artifact2] to be downloaded", executionResponse.getErrorMessage());
    assertEquals(2, executionResponse.getCorrelationIds().size());
  }
}

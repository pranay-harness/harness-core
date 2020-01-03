package software.wings.sm.states;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelayEventHelper;
import software.wings.service.intfc.ArtifactService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.UUID;

public class ArtifactCheckStateTest extends WingsBaseTest {
  @Inject private ArtifactService artifactService;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private ExecutionContext context;
  @Mock DelayEventHelper delayEventHelper;

  private String appId;
  private WorkflowStandardParams workflowStandardParams;
  private ArtifactCheckState artifactCheckState = new ArtifactCheckState("ArtifactCheckState");

  @Before
  public void setUp() throws IllegalAccessException {
    appId = UUID.randomUUID().toString();
    workflowStandardParams = aWorkflowStandardParams().withAppId(appId).build();
    FieldUtils.writeField(workflowStandardParams, "artifactService", artifactService, true);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(context.getAppId()).thenReturn(appId);
    when(delayEventHelper.delay(anyInt(), any())).thenReturn("anyGUID");
    FieldUtils.writeField(artifactCheckState, "artifactService", artifactService, true);
    FieldUtils.writeField(artifactCheckState, "delayEventHelper", delayEventHelper, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void emptyArtifacts() {
    ExecutionResponse executionResponse = artifactCheckState.execute(context);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("Artifacts are not required.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void failedArtifacts() {
    String failedArtifactId = wingsPersistence.save(anArtifact().withStatus(Status.FAILED).withAppId(appId).build());
    String errorArtifactId = wingsPersistence.save(anArtifact().withStatus(Status.ERROR).withAppId(appId).build());
    workflowStandardParams.setArtifactIds(Lists.newArrayList(failedArtifactId, errorArtifactId));

    ExecutionResponse executionResponse = artifactCheckState.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
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
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("All artifacts: [artifact1, artifact2] are available.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
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
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getErrorMessage())
        .isEqualTo("Waiting for artifacts:[artifact1, artifact2] to be downloaded");
    assertThat(executionResponse.getCorrelationIds()).hasSize(2);
  }
}

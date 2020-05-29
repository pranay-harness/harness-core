package io.harness.artifact;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.artifact.ArtifactCollectionTaskParams;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class ArtifactCollectionPTaskServiceClientTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String artifactStreamId = "ARTIFACT_STREAM_ID";
  private String taskId = "TASK_ID";
  private Map<String, String> clientParamsMap = new HashMap<>();

  private static final String ARTIFACT_STREAM_ID = "artifactStreamId";

  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private ArtifactCollectionUtils artifactCollectionUtils;

  @Inject @InjectMocks private ArtifactCollectionPTaskServiceClient artifactCollectionPTaskServiceClient;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    when(perpetualTaskService.createTask(eq(PerpetualTaskType.ARTIFACT_COLLECTION), eq(accountId),
             isA(PerpetualTaskClientContext.class), isA(PerpetualTaskSchedule.class), eq(false)))
        .thenReturn(taskId);

    clientParamsMap.put(ARTIFACT_STREAM_ID, artifactStreamId);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetTaskParams() {
    final PerpetualTaskClientContext perpetualTaskClientContext = new PerpetualTaskClientContext(clientParamsMap);
    ArtifactCollectionTaskParams collectionTaskParams =
        artifactCollectionPTaskServiceClient.getTaskParams(perpetualTaskClientContext);
    assertThat(collectionTaskParams).isNotNull();
    assertThat(collectionTaskParams.getArtifactStreamId()).isEqualTo(artifactStreamId);
    assertThat(collectionTaskParams.getBuildSourceParams()).isNotEmpty();
    verify(artifactCollectionUtils).prepareBuildSourceParameters(artifactStreamId);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCreatePerpetualTask() {
    ArtifactCollectionPTaskClientParams taskClientParams =
        ArtifactCollectionPTaskClientParams.builder().artifactStreamId(artifactStreamId).build();

    assertThat(artifactCollectionPTaskServiceClient.create(accountId, taskClientParams)).isEqualTo(taskId);

    verify(perpetualTaskService)
        .createTask(eq(PerpetualTaskType.ARTIFACT_COLLECTION), eq(accountId), isA(PerpetualTaskClientContext.class),
            isA(PerpetualTaskSchedule.class), eq(false));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetValidationTask() {
    PerpetualTaskClientContext perpetualTaskClientContext = new PerpetualTaskClientContext(clientParamsMap);
    when(artifactCollectionUtils.prepareValidateTask(artifactStreamId)).thenReturn(DelegateTask.builder().build());
    assertThat(artifactCollectionPTaskServiceClient.getValidationTask(perpetualTaskClientContext, accountId))
        .isNotNull();
    verify(artifactCollectionUtils).prepareValidateTask(artifactStreamId);
  }
}
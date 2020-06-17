package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.VGLIJIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.START;
import static software.wings.helpers.ext.gcb.models.GcbBuildStatus.SUCCESS;
import static software.wings.sm.states.GcbState.GcbDelegateResponse.gcbDelegateResponseOf;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.GcbService;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildStatus;
import software.wings.helpers.ext.gcb.models.OperationMeta;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.sm.states.GcbState.GcbDelegateResponse;

import java.util.ArrayList;
import java.util.List;
public class GcbTaskTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private GcbService gcbService;

  private static final String ACTIVITY_ID = "activityId";
  private static final String BRANCH_NAME = "branchName";
  private static final String PROJECT_ID = "projectId";
  private static final String TRIGGER_ID = "triggerId";
  private final GcpConfig gcpConfig = GcpConfig.builder().build();

  @InjectMocks
  private final GcbTask task = spy((GcbTask) TaskType.GCB.getDelegateRunnableTask(
      DelegateTaskPackage.builder()
          .delegateId("delid1")
          .delegateTask(DelegateTask.builder()
                            .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                            .build())
          .build(),
      notifyResponseData -> {}, () -> true));

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldThrowNotImplementedException() {
    assertThatThrownBy(() -> task.run(Mockito.mock(TaskParameters.class))).isInstanceOf(NotImplementedException.class);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldDelegateToRunGcbTaskParams() {
    GcbTaskParams gcbTaskParams = mock(GcbTaskParams.class);
    doReturn(gcbDelegateResponseOf(GcbTaskParams.builder().build(), GcbBuildDetails.builder().status(SUCCESS).build()))
        .when(task)
        .run(gcbTaskParams);
    task.run(new Object[] {gcbTaskParams});
    verify(task).run(gcbTaskParams);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldCallStart() {
    GcbTaskParams gcbTaskParams = GcbTaskParams.builder().type(START).build();
    GcbDelegateResponse delegateResponse = gcbDelegateResponseOf(
        GcbTaskParams.builder().build(), GcbBuildDetails.builder().status(GcbBuildStatus.WORKING).build());
    doReturn(delegateResponse).when(task).startGcbBuild(gcbTaskParams);
    GcbDelegateResponse actual = task.run(gcbTaskParams);
    verify(task).startGcbBuild(gcbTaskParams);
    assertThat(actual).isEqualTo(delegateResponse);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldExecuteSuccessfullyWhenBuildPasses() {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    RepoSource repoSource = new RepoSource();
    repoSource.setBranchName(BRANCH_NAME);
    GcbTaskParams taskParams = GcbTaskParams.builder()
                                   .type(START)
                                   .activityId(ACTIVITY_ID)
                                   .gcpConfig(gcpConfig)
                                   .encryptedDataDetails(encryptedDataDetails)
                                   .branchName(BRANCH_NAME)
                                   .projectId(PROJECT_ID)
                                   .triggerId(TRIGGER_ID)
                                   .build();
    BuildOperationDetails buildOperationDetails = new BuildOperationDetails();
    OperationMeta operationMeta = new OperationMeta();
    operationMeta.setBuild(GcbBuildDetails.builder().status(SUCCESS).build());
    buildOperationDetails.setOperationMeta(operationMeta);
    when(gcbService.runTrigger(gcpConfig, encryptedDataDetails, PROJECT_ID, TRIGGER_ID, repoSource))
        .thenReturn(buildOperationDetails);
    GcbDelegateResponse response = task.run(taskParams);
    verify(gcbService).runTrigger(gcpConfig, encryptedDataDetails, PROJECT_ID, TRIGGER_ID, repoSource);
    assertThat(response).isNotNull();
    //    assertThat(response.getActivityId()).isEqualTo(ACTIVITY_ID);
  }
}

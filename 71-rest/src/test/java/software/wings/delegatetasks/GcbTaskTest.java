package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.asyncTaskData;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.VGLIJIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.TaskType.GCB;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.POLL;
import static software.wings.beans.command.GcbTaskParams.GcbTaskType.START;
import static software.wings.sm.states.GcbState.GcbDelegateResponse.gcbDelegateResponseOf;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.Functions;
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
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.GcbService;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildStatus;
import software.wings.helpers.ext.gcb.models.OperationMeta;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.sm.states.GcbState.GcbDelegateResponse;
import software.wings.sm.states.gcbconfigs.GcbOptions;
import software.wings.sm.states.gcbconfigs.GcbTriggerBuildSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GcbTaskTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private GcbService gcbService;
  @Mock private DelegateLogService logService;

  private static final String ACTIVITY_ID = "activityId";
  private static final String BRANCH_NAME = "branchName";
  private static final String PROJECT_ID = "projectId";
  private static final String TRIGGER_ID = "triggerId";
  private static final String BUILD_ID = "buildId";
  private final GcpConfig gcpConfig = GcpConfig.builder().build();

  @InjectMocks
  private final GcbTask task = spy((GcbTask) GCB.getDelegateRunnableTask(
      DelegateTaskPackage.builder()
          .delegateId("delid1")
          .delegateTask(DelegateTask.builder().data(asyncTaskData(GCB.name())).build())
          .build(),
      Functions::doNothing, Functions::staticTruth));

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
    doReturn(gcbDelegateResponseOf(
                 GcbTaskParams.builder().build(), GcbBuildDetails.builder().status(GcbBuildStatus.SUCCESS).build()))
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
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldCallPollGcbTask() {
    GcbTaskParams gcbTaskParams = GcbTaskParams.builder().type(POLL).build();
    GcbDelegateResponse delegateResponse = gcbDelegateResponseOf(
        GcbTaskParams.builder().build(), GcbBuildDetails.builder().status(GcbBuildStatus.WORKING).build());
    doReturn(delegateResponse).when(task).pollGcbBuild(gcbTaskParams);
    GcbDelegateResponse actual = task.run(gcbTaskParams);
    verify(task).pollGcbBuild(gcbTaskParams);
    assertThat(actual).isEqualTo(delegateResponse);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldExecuteSuccessfullyWhenBuildPasses() {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    RepoSource repoSource = new RepoSource();
    GcbOptions gcbOptions = new GcbOptions();
    GcbTriggerBuildSpec triggerConfig = new GcbTriggerBuildSpec();
    triggerConfig.setName(TRIGGER_ID);
    triggerConfig.setSource(GcbTriggerBuildSpec.GcbTriggerSource.BRANCH);
    triggerConfig.setSourceId(BRANCH_NAME);
    gcbOptions.setTriggerSpec(triggerConfig);
    gcbOptions.setProjectId(PROJECT_ID);
    repoSource.setBranchName(BRANCH_NAME);
    GcbTaskParams taskParams = GcbTaskParams.builder()
                                   .type(START)
                                   .activityId(ACTIVITY_ID)
                                   .gcpConfig(gcpConfig)
                                   .encryptedDataDetails(encryptedDataDetails)
                                   .gcbOptions(gcbOptions)
                                   .build();
    BuildOperationDetails buildOperationDetails = new BuildOperationDetails();
    OperationMeta operationMeta = new OperationMeta();
    operationMeta.setBuild(GcbBuildDetails.builder().status(GcbBuildStatus.SUCCESS).build());
    buildOperationDetails.setOperationMeta(operationMeta);
    when(gcbService.runTrigger(gcpConfig, encryptedDataDetails, PROJECT_ID, TRIGGER_ID, repoSource))
        .thenReturn(buildOperationDetails);
    GcbDelegateResponse response = task.run(taskParams);
    verify(gcbService).runTrigger(gcpConfig, encryptedDataDetails, PROJECT_ID, TRIGGER_ID, repoSource);
    assertThat(response).isNotNull();
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldCallSaveLogs() {
    AtomicInteger logsCount = new AtomicInteger(0);
    task.saveConsoleLogs(
        logsCount, "activityId", "stateName", CommandExecutionStatus.SUCCESS, "appId", "line1\r\nline1\nline1");
    verify(logService, times(3))
        .save(task.getAccountId(),
            aLog()
                .withActivityId("activityId")
                .withCommandUnitName("stateName")
                .withAppId("appId")
                .withLogLevel(INFO)
                .withLogLine("line1")
                .withExecutionResult(CommandExecutionStatus.SUCCESS)
                .build());
    assertThat(logsCount.get()).isEqualTo(3);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldWaitForTheBuildToComplete() {
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    GcbOptions gcbOptions = new GcbOptions();
    GcbTriggerBuildSpec triggerBuildSpec = new GcbTriggerBuildSpec();
    triggerBuildSpec.setSource(GcbTriggerBuildSpec.GcbTriggerSource.BRANCH);
    triggerBuildSpec.setSourceId(BRANCH_NAME);
    triggerBuildSpec.setName(TRIGGER_ID);
    gcbOptions.setTriggerSpec(triggerBuildSpec);
    gcbOptions.setProjectId(PROJECT_ID);

    GcbTaskParams taskParams = GcbTaskParams.builder()
                                   .type(POLL)
                                   .activityId(ACTIVITY_ID)
                                   .gcpConfig(gcpConfig)
                                   .encryptedDataDetails(encryptedDataDetails)
                                   .gcbOptions(gcbOptions)
                                   .buildId(BUILD_ID)
                                   .pollFrequency(0)
                                   .build();

    GcbBuildDetails working =
        GcbBuildDetails.builder().status(GcbBuildStatus.WORKING).id(BUILD_ID).logsBucket("logsBucket").build();
    GcbBuildDetails success =
        GcbBuildDetails.builder().status(GcbBuildStatus.SUCCESS).id(BUILD_ID).logsBucket("logsBucket").build();

    when(gcbService.getBuild(gcpConfig, encryptedDataDetails, PROJECT_ID, BUILD_ID))
        .thenReturn(working)
        .thenReturn(working)
        .thenReturn(success);
    when(gcbService.fetchBuildLogs(gcpConfig, encryptedDataDetails, success.getLogsBucket(), success.getId()))
        .thenReturn("working")
        .thenReturn("working")
        .thenReturn("success");

    doNothing().when(task).saveConsoleLogs(any(), anyString(), anyString(), any(), anyString(), anyString());

    GcbDelegateResponse response = task.pollGcbBuild(taskParams);
    verify(gcbService, times(3)).getBuild(gcpConfig, encryptedDataDetails, PROJECT_ID, BUILD_ID);
    verify(gcbService, times(3))
        .fetchBuildLogs(gcpConfig, encryptedDataDetails, success.getLogsBucket(), success.getId());
    verify(task, times(3)).saveConsoleLogs(any(), anyString(), anyString(), any(), anyString(), anyString());
    assertThat(response).isNotNull();
    assertThat(response.getBuild()).isEqualTo(success);
  }
}

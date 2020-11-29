package software.wings.delegatetasks.aws.ecs.ecstaskhandler.deploy;

import static io.harness.rule.OwnerRule.RAGHVENDRA;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.ecs.request.EcsRunTaskDeployRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsRunTaskDeployResponse;
import software.wings.service.impl.AwsHelperService;

import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.RunTaskRequest;
import com.amazonaws.services.ecs.model.RunTaskResult;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EcsRunTaskDeployCommandHandlerTest extends WingsBaseTest {
  @Mock private EcsDeployCommandTaskHelper mockEcsDeployCommandTaskHelper;
  @Mock private AwsHelperService mockAwsHelperService;

  @InjectMocks @Inject private EcsRunTaskDeployCommandHandler ecsRunTaskDeployCommandHandler;

  private String runTaskFamilyName = "RUN_TASK_FAMILY_NAME";
  private String runTaskDefinition = "{\n"
      + "  \"family\": \"raghav_test_run_task\",\n"
      + "  \"containerDefinitions\": [\n"
      + "    {\n"
      + "      \"name\": \"web\",\n"
      + "      \"image\": \"tongueroo/sinatra:latest\",\n"
      + "      \"cpu\": 128,\n"
      + "      \"memoryReservation\": 128,\n"
      + "      \"portMappings\": [\n"
      + "        {\n"
      + "          \"containerPort\": 4567,\n"
      + "          \"protocol\": \"tcp\"\n"
      + "        }\n"
      + "      ],\n"
      + "      \"command\": [\n"
      + "        \"ruby\", \"hi.rb\"\n"
      + "      ],\n"
      + "      \"essential\": true\n"
      + "    }\n"
      + "  ]\n"
      + "}";

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalWithNoSteadyStateCheck() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());

    EcsRunTaskDeployResponse ecsRunTaskDeployResponse = EcsRunTaskDeployResponse.builder()
                                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                            .output(StringUtils.EMPTY)
                                                            .build();
    doReturn(ecsRunTaskDeployResponse).when(mockEcsDeployCommandTaskHelper).getEmptyRunTaskDeployResponse();

    when(mockEcsDeployCommandTaskHelper.createRunTaskDefinition(runTaskDefinition, runTaskFamilyName))
        .thenCallRealMethod();

    TaskDefinition registeredRunTaskDefinition = new TaskDefinition();
    registeredRunTaskDefinition.setTaskDefinitionArn("taskArn1");
    doReturn(registeredRunTaskDefinition)
        .when(mockEcsDeployCommandTaskHelper)
        .registerRunTaskDefinition(any(), any(), any(), anyString(), any(), any());

    EcsRunTaskDeployRequest ecsCommandRequest = EcsRunTaskDeployRequest.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .runTaskFamilyName(runTaskFamilyName)
                                                    .serviceSteadyStateTimeout(1l)
                                                    .skipSteadyStateCheck(true)
                                                    .launchType("EC2")
                                                    .cluster("RUN_TASK_CLUSTER")
                                                    .listTaskDefinitionJson(singletonList(runTaskDefinition))
                                                    .build();

    when(mockEcsDeployCommandTaskHelper.createAwsRunTaskRequest(registeredRunTaskDefinition, ecsCommandRequest))
        .thenCallRealMethod();

    RunTaskResult runTaskResult = new RunTaskResult();
    Task task = new Task();
    task.setTaskArn("taskArn1");
    runTaskResult.setTasks(singletonList(task));

    doReturn(runTaskResult)
        .when(mockEcsDeployCommandTaskHelper)
        .triggerRunTask(eq(ecsCommandRequest.getRegion()), any(), any(), anyObject());

    AwsConfig awsConfig = AwsConfig.builder().build();
    doReturn(awsConfig).when(mockAwsHelperService).validateAndGetAwsConfig(any(), any(), anyBoolean());

    EcsCommandExecutionResponse response =
        ecsRunTaskDeployCommandHandler.executeTaskInternal(ecsCommandRequest, null, mockCallback);

    ArgumentCaptor<RunTaskRequest> captor = ArgumentCaptor.forClass(RunTaskRequest.class);
    verify(mockEcsDeployCommandTaskHelper).triggerRunTask(anyString(), any(), any(), captor.capture());

    verify(mockEcsDeployCommandTaskHelper, times(0))
        .getTasksFromTaskArn(any(), anyString(), anyString(), any(), any(), any());

    RunTaskRequest triggeredRunTaskRequest = captor.getValue();
    assertThat(triggeredRunTaskRequest.getTaskDefinition()).isEqualTo("taskArn1");
    assertThat(triggeredRunTaskRequest.getLaunchType()).isEqualTo("EC2");
    assertThat(triggeredRunTaskRequest.getCluster()).isEqualTo("RUN_TASK_CLUSTER");
    assertThat(response).isNotNull();
    assertThat(response.getErrorMessage()).isEqualTo(null);
    assertThat(ecsRunTaskDeployResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(ecsRunTaskDeployResponse.getOutput()).isEqualTo("");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalWithSteadyStateCheck() {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());

    EcsRunTaskDeployResponse ecsRunTaskDeployResponse = EcsRunTaskDeployResponse.builder()
                                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                            .output(StringUtils.EMPTY)
                                                            .build();
    doReturn(ecsRunTaskDeployResponse).when(mockEcsDeployCommandTaskHelper).getEmptyRunTaskDeployResponse();

    when(mockEcsDeployCommandTaskHelper.createRunTaskDefinition(runTaskDefinition, runTaskFamilyName))
        .thenCallRealMethod();

    TaskDefinition registeredRunTaskDefinition = new TaskDefinition();
    registeredRunTaskDefinition.setTaskDefinitionArn("taskArn1");
    doReturn(registeredRunTaskDefinition)
        .when(mockEcsDeployCommandTaskHelper)
        .registerRunTaskDefinition(any(), any(), any(), anyString(), any(), any());

    EcsRunTaskDeployRequest ecsCommandRequest = EcsRunTaskDeployRequest.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .runTaskFamilyName(runTaskFamilyName)
                                                    .serviceSteadyStateTimeout(1l)
                                                    .launchType("EC2")
                                                    .cluster("RUN_TASK_CLUSTER")
                                                    .listTaskDefinitionJson(singletonList(runTaskDefinition))
                                                    .build();

    when(mockEcsDeployCommandTaskHelper.createAwsRunTaskRequest(registeredRunTaskDefinition, ecsCommandRequest))
        .thenCallRealMethod();

    RunTaskResult runTaskResult = new RunTaskResult();
    Task task = new Task();
    task.setTaskArn("taskArn1");
    runTaskResult.setTasks(singletonList(task));

    doReturn(runTaskResult)
        .when(mockEcsDeployCommandTaskHelper)
        .triggerRunTask(eq(ecsCommandRequest.getRegion()), any(), any(), anyObject());

    AwsConfig awsConfig = AwsConfig.builder().build();
    doReturn(awsConfig).when(mockAwsHelperService).validateAndGetAwsConfig(any(), any(), anyBoolean());

    task.setLastStatus(DesiredStatus.STOPPED.name());

    Task taskWithStatusStopped = new Task();
    taskWithStatusStopped.setTaskArn("taskArn1");
    taskWithStatusStopped.setLastStatus(DesiredStatus.RUNNING.name());
    doReturn(singletonList(taskWithStatusStopped))
        .doReturn(singletonList(task))
        .when(mockEcsDeployCommandTaskHelper)
        .getTasksFromTaskArn(eq(awsConfig), anyString(), anyString(), eq(singletonList("taskArn1")), any(), any());

    EcsCommandExecutionResponse response =
        ecsRunTaskDeployCommandHandler.executeTaskInternal(ecsCommandRequest, null, mockCallback);

    ArgumentCaptor<RunTaskRequest> captor = ArgumentCaptor.forClass(RunTaskRequest.class);
    verify(mockEcsDeployCommandTaskHelper, times(1)).triggerRunTask(anyString(), any(), any(), captor.capture());

    verify(mockEcsDeployCommandTaskHelper, times(2))
        .getTasksFromTaskArn(any(), anyString(), anyString(), any(), any(), any());

    RunTaskRequest triggeredRunTaskRequest = captor.getValue();
    assertThat(triggeredRunTaskRequest.getTaskDefinition()).isEqualTo("taskArn1");
    assertThat(triggeredRunTaskRequest.getLaunchType()).isEqualTo("EC2");
    assertThat(triggeredRunTaskRequest.getCluster()).isEqualTo("RUN_TASK_CLUSTER");
    assertThat(response).isNotNull();
    assertThat(response.getErrorMessage()).isEqualTo(null);
    assertThat(ecsRunTaskDeployResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(ecsRunTaskDeployResponse.getOutput()).isEqualTo("");
  }
}

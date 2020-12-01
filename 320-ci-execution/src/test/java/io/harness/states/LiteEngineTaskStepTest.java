package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIK8BuildTaskParams;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.CiK8sTaskResponse;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.k8s.PodStatus;
import io.harness.delegate.task.HDelegateTask;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.executionplan.CIExecutionTest;
import io.harness.k8s.model.ImageDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class LiteEngineTaskStepTest extends CIExecutionTest {
  @Mock private BuildSetupUtils buildSetupUtils;
  @Mock private Map<String, TaskExecutor<HDelegateTask>> taskExecutorMap;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @InjectMocks private LiteEngineTaskStep liteEngineTaskStep;

  private Ambiance ambiance;
  private LiteEngineTaskStepInfo liteEngineTaskStepInfo;

  @Before
  public void setUp() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    ambiance = Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
    liteEngineTaskStepInfo =
        LiteEngineTaskStepInfo.builder()
            .steps(
                ExecutionElement.builder()
                    .steps(asList(StepElement.builder()
                                      .type("restoreCache")
                                      .stepSpecType(RestoreCacheStepInfo.builder().identifier("restoreCache").build())
                                      .build(),
                        StepElement.builder()
                            .type("run")
                            .stepSpecType(RunStepInfo.builder().identifier("run").build())
                            .build(),
                        ParallelStepElement.builder()
                            .sections(asList(StepElement.builder()
                                                 .type("publishArtifacts")
                                                 .stepSpecType(PublishStepInfo.builder().identifier("publish").build())
                                                 .build(),
                                StepElement.builder()
                                    .type("saveCache")
                                    .stepSpecType(SaveCacheStepInfo.builder().identifier("saveCache").build())
                                    .build()))
                            .build()))
                    .build())
            .build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldObtainTask() {
    TaskExecutor<HDelegateTask> executor = mock(TaskExecutor.class);
    when(taskExecutorMap.get(TaskMode.DELEGATE_TASK_V3.name())).thenReturn(executor);
    when(executor.queueTask(eq(ambiance.getSetupAbstractionsMap()), any())).thenReturn("taskId");

    when(buildSetupUtils.getBuildSetupTaskParams(eq(liteEngineTaskStepInfo), eq(ambiance)))
        .thenReturn(CIK8BuildTaskParams.builder().build());

    Task task = liteEngineTaskStep.obtainTask(ambiance, liteEngineTaskStepInfo, StepInputPackage.builder().build());

    assertThat(task).isNotNull().isInstanceOf(HDelegateTask.class);
    TaskData taskData = ((HDelegateTask) task).getData();
    assertThat(taskData.getTaskType()).isEqualTo("CI_BUILD");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldHandleSuccessfulTaskResult() {
    PodStatus podStatus = PodStatus.builder().build();
    CiK8sTaskResponse taskResponse = CiK8sTaskResponse.builder().podName("test").podStatus(podStatus).build();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    responseDataMap.put("waitId",
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .k8sTaskResponse(taskResponse)
            .build());

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(null);
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResult(ambiance, liteEngineTaskStepInfo, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldHandleFailedTaskResult() {
    PodStatus podStatus = PodStatus.builder().build();
    CiK8sTaskResponse taskResponse = CiK8sTaskResponse.builder().podName("test").podStatus(podStatus).build();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    responseDataMap.put("waitId",
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .k8sTaskResponse(taskResponse)
            .build());

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(null);
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResult(ambiance, liteEngineTaskStepInfo, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void shouldHandleSuccessfulTaskResultWithServices() {
    String containerName = "ctr";
    String errMsg = "Terminated";
    String image = "redis:latest";
    String stepId = "cache";
    String stepName = "cache";
    CIContainerStatus ciContainerStatus = CIContainerStatus.builder()
                                              .status(CIContainerStatus.Status.ERROR)
                                              .name(containerName)
                                              .errorMsg(errMsg)
                                              .image(image)
                                              .build();
    PodStatus podStatus = PodStatus.builder().ciContainerStatusList(Arrays.asList(ciContainerStatus)).build();
    CiK8sTaskResponse taskResponse = CiK8sTaskResponse.builder().podName("test").podStatus(podStatus).build();

    ContainerDefinitionInfo serviceContainer =
        ContainerDefinitionInfo.builder().stepIdentifier(stepId).stepName(stepName).name(containerName).build();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    responseDataMap.put("waitId",
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .k8sTaskResponse(taskResponse)
            .build());

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(Arrays.asList(serviceContainer));
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResult(ambiance, liteEngineTaskStepInfo, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void shouldHandleSuccessfulTaskResultWithServicesNoPodStatus() {
    String containerName = "ctr";
    String stepId = "cache";
    String stepName = "cache";
    PodStatus podStatus = PodStatus.builder().build();
    CiK8sTaskResponse taskResponse = CiK8sTaskResponse.builder().podName("test").podStatus(podStatus).build();

    ContainerDefinitionInfo serviceContainer =
        ContainerDefinitionInfo.builder()
            .stepIdentifier(stepId)
            .stepName(stepName)
            .name(containerName)
            .containerImageDetails(
                ContainerImageDetails.builder().imageDetails(ImageDetails.builder().name("redis").build()).build())
            .build();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    responseDataMap.put("waitId",
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .k8sTaskResponse(taskResponse)
            .build());

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(Arrays.asList(serviceContainer));
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResult(ambiance, liteEngineTaskStepInfo, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }
}

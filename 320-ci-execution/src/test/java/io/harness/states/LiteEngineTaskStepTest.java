/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.CiK8sTaskResponse;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.k8s.PodStatus;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.k8s.model.ImageDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.stateutils.buildstate.BuildSetupUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CI)
public class LiteEngineTaskStepTest extends CIExecutionTestBase {
  @Mock private BuildSetupUtils buildSetupUtils;
  @Mock private CIDelegateTaskExecutor ciDelegateTaskExecutor;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Mock private KryoSerializer kryoSerializer;
  @InjectMocks private LiteEngineTaskStep liteEngineTaskStep;

  private Ambiance ambiance;
  private LiteEngineTaskStepInfo liteEngineTaskStepInfo;
  private StepElementParameters stepElementParameters;

  @Before
  public void setUp() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    ambiance = Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
    liteEngineTaskStepInfo = LiteEngineTaskStepInfo.builder().build();
    stepElementParameters = StepElementParameters.builder().name("name").spec(liteEngineTaskStepInfo).build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldObtainTask() {
    when(ciDelegateTaskExecutor.queueTask(eq(ambiance.getSetupAbstractionsMap()), any())).thenReturn("taskId");

    //    when(buildSetupUtils.getBuildSetupTaskParams(eq(liteEngineTaskStepInfo), eq(ambiance)))
    //        .thenReturn(CIK8BuildTaskParams.builder().build());

    //    TaskRequest taskRequest =
    //        liteEngineTaskStep.obtainTask(ambiance, liteEngineTaskStepInfo, StepInputPackage.builder().build());
    //
    //    assertThat(taskRequest.getDelegateTaskRequest()).isNotNull();
    //    TaskType taskType = taskRequest.getDelegateTaskRequest().getDetails().getType();
    //    assertThat(taskType.getType()).isEqualTo("CI_BUILD");
  }

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldHandleSuccessfulTaskResult() {
    PodStatus podStatus = PodStatus.builder().build();
    CiK8sTaskResponse taskResponse = CiK8sTaskResponse.builder().podName("test").podStatus(podStatus).build();
    K8sTaskExecutionResponse executionResponse = K8sTaskExecutionResponse.builder()
                                                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                     .k8sTaskResponse(taskResponse)
                                                     .build();

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(null);
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> executionResponse);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldHandleFailedTaskResult() {
    PodStatus podStatus = PodStatus.builder().build();
    CiK8sTaskResponse taskResponse = CiK8sTaskResponse.builder().podName("test").podStatus(podStatus).build();

    K8sTaskExecutionResponse executionResponse = K8sTaskExecutionResponse.builder()
                                                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                                     .k8sTaskResponse(taskResponse)
                                                     .build();

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(null);
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> executionResponse);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }

  @SneakyThrows
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
    K8sTaskExecutionResponse executionResponse = K8sTaskExecutionResponse.builder()
                                                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                     .k8sTaskResponse(taskResponse)
                                                     .build();

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(Arrays.asList(serviceContainer));
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> executionResponse);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @SneakyThrows
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
    K8sTaskExecutionResponse executionResponse = K8sTaskExecutionResponse.builder()
                                                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                     .k8sTaskResponse(taskResponse)
                                                     .build();

    when(buildSetupUtils.getBuildServiceContainers(liteEngineTaskStepInfo)).thenReturn(Arrays.asList(serviceContainer));
    StepResponse stepResponse = liteEngineTaskStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> executionResponse);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }
}

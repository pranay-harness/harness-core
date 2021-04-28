package io.harness.states;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.outcomes.LiteEnginePodDetailsOutcome.POD_DETAILS_OUTCOME;
import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.LOG_KEYS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.dependencies.ServiceDependency;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.outcomes.DependencyOutcome;
import io.harness.beans.outcomes.LiteEnginePodDetailsOutcome;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.StepLogKeyDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.connector.accesscontrol.ConnectorsAccessControlPermissions;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.CiK8sTaskResponse;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.k8s.model.ImageDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logstreaming.LogStreamingHelper;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PrincipalTypeProtoToPrincipalTypeMapper;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.serializer.KryoSerializer;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;
import io.harness.steps.executable.TaskExecutableWithRbac;
import io.harness.supplier.ThrowingSupplier;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * This state will setup the build environment, clone the git repository for running CI job.
 */

@Slf4j
@OwnedBy(CI)
public class LiteEngineTaskStep implements TaskExecutableWithRbac<StepElementParameters, K8sTaskExecutionResponse> {
  public static final String TASK_TYPE_CI_BUILD = "CI_BUILD";
  public static final String LE_STATUS_TASK_TYPE = "CI_LE_STATUS";
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject @Named("PRIVILEGED") AccessControlClient accessControlClient;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private CIDelegateTaskExecutor ciDelegateTaskExecutor;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;

  private static final String DEPENDENCY_OUTCOME = "dependencies";
  public static final StepType STEP_TYPE = LiteEngineTaskStepInfo.STEP_TYPE;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepElementParameters) {
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    String principal = executionPrincipalInfo.getPrincipal();
    if (EmptyPredicate.isEmpty(principal)) {
      return;
    }

    LiteEngineTaskStepInfo liteEngineTaskStepInfo = (LiteEngineTaskStepInfo) stepElementParameters.getSpec();
    List<PermissionCheckDTO> connectorPermissionCheckDTOs =
        getConnectorIdentifiers(liteEngineTaskStepInfo, accountIdentifier, projectIdentifier, orgIdentifier);

    PrincipalType principalType = PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(
        executionPrincipalInfo.getPrincipalType());

    Optional<AccessCheckResponseDTO> accessCheckResponseDTO;
    try {
      RetryPolicy<Object> retryPolicy =
          getRetryPolicy(format("[Retrying failed call to check connector permissions attempt: {}"),
              format("Failed to check connector permissions after retrying {} times"));

      accessCheckResponseDTO =
          Failsafe.with(retryPolicy)
              .get(()
                       -> Optional.of(accessControlClient.checkForAccess(
                           Principal.builder().principalIdentifier(principal).principalType(principalType).build(),
                           connectorPermissionCheckDTOs)));

    } catch (Exception e) {
      log.error("Unable to check access for connectors", e);
      throw new CIStageExecutionException("Unable to check access for connectors");
    }

    if (!accessCheckResponseDTO.isPresent()) {
      throw new CIStageExecutionException("Unable to check access for connectors");
    }
    List<String> connectorsWithoutPermissions =
        accessCheckResponseDTO.get()
            .getAccessControlList()
            .stream()
            .filter(accessControlDTO -> { return !accessControlDTO.isPermitted(); })
            .map(AccessControlDTO::getResourceIdentifier)
            .collect(Collectors.toList());

    if (!connectorsWithoutPermissions.isEmpty()) {
      String connectorsWithoutPermissionsString = String.join(",", connectorsWithoutPermissions);

      String errorMessage = format("Validation for initialise Step failed, No runtime access on connectors [%s]",
          connectorsWithoutPermissionsString);
      throw new AccessDeniedException(errorMessage, ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    LiteEngineTaskStepInfo stepParameters = (LiteEngineTaskStepInfo) stepElementParameters.getSpec();

    Map<String, String> taskIds = new HashMap<>();
    String logPrefix = getLogPrefix(ambiance);
    Map<String, String> stepLogKeys = getStepLogKeys(stepParameters, ambiance, logPrefix);

    CIBuildSetupTaskParams buildSetupTaskParams =
        buildSetupUtils.getBuildSetupTaskParams(stepParameters, ambiance, taskIds, logPrefix, stepLogKeys);
    log.info("Created params for build task: {}", buildSetupTaskParams);

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(stepParameters.getTimeout())
                                  .taskType(TASK_TYPE_CI_BUILD)
                                  .parameters(new Object[] {buildSetupTaskParams})
                                  .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer);
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepElementParameters,
      ThrowingSupplier<K8sTaskExecutionResponse> responseSupplier) throws Exception {
    K8sTaskExecutionResponse k8sTaskExecutionResponse = responseSupplier.get();

    LiteEngineTaskStepInfo stepParameters = (LiteEngineTaskStepInfo) stepElementParameters.getSpec();

    DependencyOutcome dependencyOutcome =
        getDependencyOutcome(ambiance, stepParameters, k8sTaskExecutionResponse.getK8sTaskResponse());
    LiteEnginePodDetailsOutcome liteEnginePodDetailsOutcome =
        getPodDetailsOutcome(k8sTaskExecutionResponse.getK8sTaskResponse());

    StepResponse.StepOutcome stepOutcome =
        StepResponse.StepOutcome.builder().name(DEPENDENCY_OUTCOME).outcome(dependencyOutcome).build();
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      log.info(
          "LiteEngineTaskStep pod creation task executed successfully with response [{}]", k8sTaskExecutionResponse);
      if (liteEnginePodDetailsOutcome == null) {
        throw new CIStageExecutionException("Failed to get pod local ipAddress details");
      }
      return StepResponse.builder()
          .status(Status.SUCCEEDED)
          .stepOutcome(stepOutcome)
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .name(POD_DETAILS_OUTCOME)
                           .group(StepOutcomeGroup.STAGE.name())
                           .outcome(liteEnginePodDetailsOutcome)
                           .build())
          .build();

    } else {
      log.error("LiteEngineTaskStep execution finished with status [{}] and response [{}]",
          k8sTaskExecutionResponse.getCommandExecutionStatus(), k8sTaskExecutionResponse);

      StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(Status.FAILED).stepOutcome(stepOutcome);
      if (k8sTaskExecutionResponse.getErrorMessage() != null) {
        stepResponseBuilder.failureInfo(
            FailureInfo.newBuilder().setErrorMessage(k8sTaskExecutionResponse.getErrorMessage()).build());
      }
      return stepResponseBuilder.build();
    }
  }

  private LiteEnginePodDetailsOutcome getPodDetailsOutcome(CiK8sTaskResponse ciK8sTaskResponse) {
    if (ciK8sTaskResponse != null && ciK8sTaskResponse.getPodStatus() != null) {
      String ip = ciK8sTaskResponse.getPodStatus().getIp();
      return LiteEnginePodDetailsOutcome.builder().ipAddress(ip).build();
    }
    return null;
  }

  private DependencyOutcome getDependencyOutcome(
      Ambiance ambiance, LiteEngineTaskStepInfo stepParameters, CiK8sTaskResponse ciK8sTaskResponse) {
    List<ContainerDefinitionInfo> serviceContainers = buildSetupUtils.getBuildServiceContainers(stepParameters);
    List<ServiceDependency> serviceDependencyList = new ArrayList<>();
    if (serviceContainers == null) {
      return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
    }

    Map<String, CIContainerStatus> containerStatusMap = new HashMap<>();
    if (ciK8sTaskResponse != null && ciK8sTaskResponse.getPodStatus() != null
        && ciK8sTaskResponse.getPodStatus().getCiContainerStatusList() != null) {
      for (CIContainerStatus containerStatus : ciK8sTaskResponse.getPodStatus().getCiContainerStatusList()) {
        containerStatusMap.put(containerStatus.getName(), containerStatus);
      }
    }

    String logPrefix = getLogPrefix(ambiance);
    for (ContainerDefinitionInfo serviceContainer : serviceContainers) {
      String logKey = format("%s/serviceId:%s", logPrefix, serviceContainer.getStepIdentifier());
      String containerName = serviceContainer.getName();
      if (containerStatusMap.containsKey(containerName)) {
        CIContainerStatus containerStatus = containerStatusMap.get(containerName);

        ServiceDependency.Status status = ServiceDependency.Status.SUCCESS;
        if (containerStatus.getStatus() == CIContainerStatus.Status.ERROR) {
          status = ServiceDependency.Status.ERROR;
        }
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceContainer.getStepIdentifier())
                                      .name(serviceContainer.getStepName())
                                      .image(containerStatus.getImage())
                                      .startTime(containerStatus.getStartTime())
                                      .endTime(containerStatus.getEndTime())
                                      .errorMessage(containerStatus.getErrorMsg())
                                      .status(status)
                                      .logKeys(Collections.singletonList(logKey))
                                      .build());
      } else {
        ImageDetails imageDetails = serviceContainer.getContainerImageDetails().getImageDetails();
        String image = imageDetails.getName();
        if (isEmpty(imageDetails.getTag())) {
          image += format(":%s", imageDetails.getTag());
        }
        serviceDependencyList.add(ServiceDependency.builder()
                                      .identifier(serviceContainer.getStepIdentifier())
                                      .name(serviceContainer.getStepName())
                                      .image(image)
                                      .errorMessage("Unknown")
                                      .status(ServiceDependency.Status.ERROR)
                                      .logKeys(Collections.singletonList(logKey))
                                      .build());
      }
    }
    return DependencyOutcome.builder().serviceDependencyList(serviceDependencyList).build();
  }

  private Map<String, String> getStepLogKeys(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance, String logPrefix) {
    Map<String, String> logKeyByStepId = new HashMap<>();
    liteEngineTaskStepInfo.getExecutionElementConfig().getSteps().forEach(
        executionWrapper -> addLogKey(executionWrapper, logPrefix, logKeyByStepId));

    Map<String, List<String>> logKeys = new HashMap<>();
    logKeyByStepId.forEach((stepId, logKey) -> logKeys.put(stepId, Collections.singletonList(logKey)));
    executionSweepingOutputResolver.consume(
        ambiance, LOG_KEYS, StepLogKeyDetails.builder().logKeys(logKeys).build(), StepOutcomeGroup.STAGE.name());
    return logKeyByStepId;
  }

  private void addLogKey(
      ExecutionWrapperConfig executionWrapper, String logPrefix, Map<String, String> logKeyByStepId) {
    if (executionWrapper != null) {
      if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);

        logKeyByStepId.put(stepElementConfig.getIdentifier(), getStepLogKey(stepElementConfig, logPrefix));
      } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig =
            IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
        parallelStepElementConfig.getSections().forEach(section -> addLogKey(section, logPrefix, logKeyByStepId));
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
  }

  private String getStepLogKey(StepElementConfig stepElement, String logPrefix) {
    return format("%s/stepId:%s", logPrefix, stepElement.getIdentifier());
  }

  private String getLogPrefix(Ambiance ambiance) {
    LinkedHashMap<String, String> logAbstractions = StepUtils.generateLogAbstractions(ambiance, "STAGE");
    return LogStreamingHelper.generateLogBaseKey(logAbstractions);
  }

  private List<PermissionCheckDTO> getConnectorIdentifiers(LiteEngineTaskStepInfo liteEngineTaskStepInfo,
      String accountIdentifier, String projectIdentifier, String orgIdentifier) {
    K8BuildJobEnvInfo.PodsSetupInfo podSetupInfo =
        ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPodsSetupInfo();
    if (isEmpty(podSetupInfo.getPodSetupInfoList())) {
      return new ArrayList<>();
    }
    Infrastructure infrastructure = liteEngineTaskStepInfo.getInfrastructure();
    if (infrastructure == null || ((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    List<PermissionCheckDTO> permissionCheckDTOList = new ArrayList<>();

    String infraConnectorRef = ((K8sDirectInfraYaml) infrastructure).getSpec().getConnectorRef();

    // Add Infra connector
    permissionCheckDTOList.add(PermissionCheckDTO.builder()
                                   .permission(ConnectorsAccessControlPermissions.ACCESS_CONNECTOR_PERMISSION)
                                   .resourceIdentifier(infraConnectorRef)
                                   .resourceScope(ResourceScope.builder()
                                                      .accountIdentifier(accountIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .orgIdentifier(orgIdentifier)
                                                      .build())
                                   .resourceType("CONNECTOR")
                                   .build());

    // Add git clone connector
    if (!liteEngineTaskStepInfo.isSkipGitClone()) {
      permissionCheckDTOList.add(PermissionCheckDTO.builder()
                                     .permission(ConnectorsAccessControlPermissions.ACCESS_CONNECTOR_PERMISSION)
                                     .resourceIdentifier(liteEngineTaskStepInfo.getCiCodebase().getConnectorRef())
                                     .resourceScope(ResourceScope.builder()
                                                        .accountIdentifier(accountIdentifier)
                                                        .projectIdentifier(projectIdentifier)
                                                        .orgIdentifier(orgIdentifier)
                                                        .build())
                                     .resourceType("CONNECTOR")
                                     .build());
    }

    Optional<PodSetupInfo> podSetupInfoOptional = podSetupInfo.getPodSetupInfoList().stream().findFirst();
    try {
      if (podSetupInfoOptional.isPresent()) {
        permissionCheckDTOList.addAll(podSetupInfoOptional.get()
                                          .getPodSetupParams()
                                          .getContainerDefinitionInfos()

                                          .stream()
                                          .map(ContainerDefinitionInfo::getContainerImageDetails)
                                          .map(ContainerImageDetails::getConnectorIdentifier)
                                          .filter(Objects::nonNull)
                                          .map(connectorIdentifier -> {
                                            return PermissionCheckDTO.builder()
                                                .permission(
                                                    ConnectorsAccessControlPermissions.ACCESS_CONNECTOR_PERMISSION)
                                                .resourceIdentifier(connectorIdentifier)
                                                .resourceScope(ResourceScope.builder()
                                                                   .accountIdentifier(accountIdentifier)
                                                                   .projectIdentifier(projectIdentifier)
                                                                   .orgIdentifier(orgIdentifier)
                                                                   .build())
                                                .resourceType("CONNECTOR")
                                                .build();
                                          })
                                          .collect(Collectors.toList()));
      }
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to retrieve connector information", ex);
    }

    return permissionCheckDTOList;
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}

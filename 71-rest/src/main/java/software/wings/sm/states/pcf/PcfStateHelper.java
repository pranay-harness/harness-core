package software.wings.sm.states.pcf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.pcf.model.PcfConstants.MANIFEST_YML;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.EnvironmentGlobal;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.Service;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.ServiceOverride;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.pcf.ManifestType;
import io.harness.pcf.PcfFileTypeChecker;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.service.ServiceHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class PcfStateHelper {
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ServiceHelper serviceHelper;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private PcfFileTypeChecker pcfFileTypeChecker;

  public PcfCommandRequest getPcfCommandRouteUpdateRequest(PcfCommandType pcfCommandType, String commandName,
      String appId, String accountId, String activityId, PcfConfig pcfConfig, String organization, String space,
      PcfRouteUpdateRequestConfigData requestConfigData, Integer timeoutIntervalInMin) {
    timeoutIntervalInMin = timeoutIntervalInMin == null ? Integer.valueOf(5) : timeoutIntervalInMin;
    return PcfCommandRouteUpdateRequest.builder()
        .pcfCommandType(pcfCommandType)
        .commandName(commandName)
        .appId(appId)
        .accountId(accountId)
        .activityId(activityId)
        .pcfConfig(pcfConfig)
        .organization(organization)
        .space(space)
        .pcfRouteUpdateConfigData(requestConfigData)
        .timeoutIntervalInMin(timeoutIntervalInMin)
        .build();
  }

  public DelegateTask getDelegateTask(String accountId, String appId, TaskType taskType, String waitId, String envId,
      String infrastructureMappingId, Object[] parameters, long timeout) {
    return DelegateTask.builder()
        .async(true)
        .accountId(accountId)
        .appId(appId)
        .waitId(waitId)
        .data(TaskData.builder()
                  .taskType(taskType.name())
                  .parameters(parameters)
                  .timeout(TimeUnit.MINUTES.toMillis(timeout))
                  .build())
        .envId(envId)
        .infrastructureMappingId(infrastructureMappingId)
        .build();
  }

  public PcfRouteUpdateStateExecutionData getRouteUpdateStateExecutionData(String activityId, String appId,
      String accountId, PcfCommandRequest pcfCommandRequest, String commandName,
      PcfRouteUpdateRequestConfigData requestConfigData) {
    return PcfRouteUpdateStateExecutionData.builder()
        .activityId(activityId)
        .accountId(accountId)
        .appId(appId)
        .pcfCommandRequest(pcfCommandRequest)
        .commandName(commandName)
        .pcfRouteUpdateRequestConfigData(requestConfigData)
        .build();
  }

  public ActivityBuilder getActivityBuilder(String appName, String appId, String commandName, Type type,
      ExecutionContext executionContext, String commandType, CommandUnitType commandUnitType, Environment environment,
      List<CommandUnit> commandUnits) {
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    return Activity.builder()
        .applicationName(appName)
        .appId(appId)
        .commandName(commandName)
        .type(type)
        .workflowType(executionContext.getWorkflowType())
        .workflowExecutionName(executionContext.getWorkflowExecutionName())
        .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
        .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
        .commandType(commandType)
        .workflowExecutionId(executionContext.getWorkflowExecutionId())
        .workflowId(executionContext.getWorkflowId())
        .commandUnits(commandUnits)
        .status(ExecutionStatus.RUNNING)
        .commandUnitType(commandUnitType)
        .environmentId(environment.getUuid())
        .environmentName(environment.getName())
        .environmentType(environment.getEnvironmentType())
        .triggeredBy(TriggeredBy.builder()
                         .email(workflowStandardParams.getCurrentUser().getEmail())
                         .name(workflowStandardParams.getCurrentUser().getName())
                         .build());
  }

  public ExecutionResponse queueDelegateTaskForRouteUpdate(Application app, PcfConfig pcfConfig,
      DelegateService delegateService, PcfInfrastructureMapping pcfInfrastructureMapping, String activityId,
      String envId, Integer timeoutIntervalInMinutes, String commandName,
      PcfRouteUpdateRequestConfigData requestConfigData, List<EncryptedDataDetail> encryptedDataDetails) {
    PcfCommandRequest pcfCommandRequest = getPcfCommandRouteUpdateRequest(PcfCommandType.UPDATE_ROUTE, commandName,
        app.getUuid(), app.getAccountId(), activityId, pcfConfig, pcfInfrastructureMapping.getOrganization(),
        pcfInfrastructureMapping.getSpace(), requestConfigData, timeoutIntervalInMinutes);

    PcfRouteUpdateStateExecutionData stateExecutionData = getRouteUpdateStateExecutionData(
        activityId, app.getAccountId(), app.getUuid(), pcfCommandRequest, commandName, requestConfigData);

    DelegateTask delegateTask =
        getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.PCF_COMMAND_TASK, activityId, envId,
            pcfInfrastructureMapping.getUuid(), new Object[] {pcfCommandRequest, encryptedDataDetails}, 10);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(activityId))
        .stateExecutionData(stateExecutionData)
        .async(true)
        .build();
  }

  public Activity createActivity(ExecutionContext executionContext, String commandName, String stateType,
      CommandUnitType commandUnitType, ActivityService activityService) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    notNullCheck("Application does not exist", app, USER);
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    notNullCheck("Environment does not exist", env, USER);

    ActivityBuilder activityBuilder = getActivityBuilder(app.getName(), app.getUuid(), commandName, Type.Command,
        executionContext, stateType, commandUnitType, env, Collections.emptyList());
    return activityService.save(activityBuilder.build());
  }

  public String fetchManifestYmlString(ExecutionContext context, Application app, ServiceElement serviceElement) {
    String applicationManifestYmlContent;
    if (featureFlagService.isEnabled(FeatureName.PCF_MANIFEST_REDESIGN, app.getAccountId())) {
      applicationManifestYmlContent = getManifestFileContentForPcf(app, serviceElement);
    } else {
      applicationManifestYmlContent = getManifestFromPcfServiceSpecification(context, serviceElement);
    }
    return applicationManifestYmlContent;
  }

  private String getManifestFileContentForPcf(Application app, ServiceElement serviceElement) {
    ApplicationManifest applicationManifest = applicationManifestService.getByServiceId(
        app.getUuid(), serviceElement.getUuid(), AppManifestKind.K8S_MANIFEST);

    if (applicationManifest == null) {
      throw new InvalidArgumentsException(Pair.of("applicationManifest", "Missing for PCF Service"));
    }
    ManifestFile manifestFile =
        applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), MANIFEST_YML);
    if (manifestFile == null) {
      throw new InvalidArgumentsException(Pair.of("manifestFile", "manifest.yml file missing"));
    }

    return manifestFile.getFileContent();
  }

  private String getManifestFromPcfServiceSpecification(ExecutionContext context, ServiceElement serviceElement) {
    PcfServiceSpecification pcfServiceSpecification =
        serviceResourceService.getPcfServiceSpecification(context.getAppId(), serviceElement.getUuid());

    if (pcfServiceSpecification == null) {
      throw new InvalidArgumentsException(
          Pair.of("PcfServiceSpecification", "Missing for PCF Service " + serviceElement.getUuid()));
    }

    serviceHelper.addPlaceholderTexts(pcfServiceSpecification);
    validateSpecification(pcfServiceSpecification);
    return pcfServiceSpecification.getManifestYaml();
  }

  private void validateSpecification(PcfServiceSpecification pcfServiceSpecification) {
    if (pcfServiceSpecification == null || pcfServiceSpecification.getManifestYaml() == null
        || !pcfServiceSpecification.getManifestYaml().contains("{FILE_LOCATION}")
        || !pcfServiceSpecification.getManifestYaml().contains("{INSTANCE_COUNT}")
        || !pcfServiceSpecification.getManifestYaml().contains("{APPLICATION_NAME}")) {
      throw new InvalidRequestException("Invalid manifest yaml "
              + (pcfServiceSpecification == null || pcfServiceSpecification.getManifestYaml() == null
                        ? "NULL"
                        : pcfServiceSpecification.getManifestYaml()),
          USER);
    }
  }

  public Map<ManifestType, String> getFinalManifestFilesMap(Map<K8sValuesLocation, ApplicationManifest> appManifestMap,
      GitFetchFilesFromMultipleRepoResult fetchFilesResult) {
    Map<ManifestType, String> pcfManifestFilesMap = new HashMap<>();

    ApplicationManifest applicationManifest = appManifestMap.get(Service);
    updatePcfManifestFilesMap(applicationManifest, fetchFilesResult, Service, pcfManifestFilesMap);

    applicationManifest = appManifestMap.get(ServiceOverride);
    updatePcfManifestFilesMap(applicationManifest, fetchFilesResult, ServiceOverride, pcfManifestFilesMap);

    applicationManifest = appManifestMap.get(EnvironmentGlobal);
    updatePcfManifestFilesMap(applicationManifest, fetchFilesResult, EnvironmentGlobal, pcfManifestFilesMap);

    applicationManifest = appManifestMap.get(K8sValuesLocation.Environment);
    updatePcfManifestFilesMap(
        applicationManifest, fetchFilesResult, K8sValuesLocation.Environment, pcfManifestFilesMap);

    return pcfManifestFilesMap;
  }

  private void updatePcfManifestFilesMap(ApplicationManifest applicationManifest,
      GitFetchFilesFromMultipleRepoResult fetchFilesResult, K8sValuesLocation k8sValuesLocation,
      Map<ManifestType, String> pcfManifestFilesMap) {
    if (applicationManifest == null) {
      return;
    }

    if (StoreType.Local.equals(applicationManifest.getStoreType())) {
      List<ManifestFile> manifestFiles = applicationManifestService.getManifestFilesByAppManifestId(
          applicationManifest.getAppId(), applicationManifest.getUuid());

      for (ManifestFile manifestFile : manifestFiles) {
        addToPcfManifestFilesMap(manifestFile.getFileContent(), pcfManifestFilesMap);
      }
    } else if (StoreType.Remote.equals(applicationManifest.getStoreType())) {
      if (fetchFilesResult == null || isEmpty(fetchFilesResult.getFilesFromMultipleRepo())) {
        return;
      }

      GitFetchFilesResult gitFetchFilesResult =
          fetchFilesResult.getFilesFromMultipleRepo().get(k8sValuesLocation.name());
      if (gitFetchFilesResult == null || isEmpty(gitFetchFilesResult.getFiles())) {
        return;
      }

      List<GitFile> files = gitFetchFilesResult.getFiles();
      for (GitFile gitFile : files) {
        addToPcfManifestFilesMap(gitFile.getFileContent(), pcfManifestFilesMap);
      }
    }
  }

  private void addToPcfManifestFilesMap(String fileContent, Map<ManifestType, String> pcfManifestFilesMap) {
    ManifestType manifestType = pcfFileTypeChecker.getManifestType(fileContent);
    if (manifestType == null) {
      return;
    }

    pcfManifestFilesMap.put(manifestType, fileContent);
  }
}
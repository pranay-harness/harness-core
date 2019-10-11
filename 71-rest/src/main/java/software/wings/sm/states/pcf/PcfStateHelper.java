package software.wings.sm.states.pcf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.MANIFEST_YML;
import static io.harness.pcf.model.PcfConstants.NO_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_PLACEHOLDER_TOKEN_DEPRECATED;
import static java.util.Collections.emptyList;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.EnvironmentGlobal;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.Service;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.ServiceOverride;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.pcf.ManifestType;
import io.harness.pcf.PcfFileTypeChecker;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class PcfStateHelper {
  public static final String WORKFLOW_STANDARD_PARAMS = "workflowStandardParams";
  public static final String CURRENT_USER = "currentUser";
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ServiceHelper serviceHelper;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private PcfFileTypeChecker pcfFileTypeChecker;
  @Inject private DelegateService delegateService;

  public DelegateTask getDelegateTask(PcfDelegateTaskCreationData taskCreationData) {
    return DelegateTask.builder()
        .async(true)
        .accountId(taskCreationData.getAccountId())
        .appId(taskCreationData.getAppId())
        .waitId(taskCreationData.getWaitId())
        .data(TaskData.builder()
                  .taskType(taskCreationData.getTaskType().name())
                  .parameters(taskCreationData.getParameters())
                  .timeout(TimeUnit.MINUTES.toMillis(taskCreationData.getTimeout()))
                  .build())
        .envId(taskCreationData.getEnvId())
        .infrastructureMappingId(taskCreationData.getInfrastructureMappingId())
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

  public ActivityBuilder getActivityBuilder(PcfActivityBuilderCreationData activityBuilderCreationData) {
    ExecutionContext executionContext = activityBuilderCreationData.getExecutionContext();
    Environment environment = activityBuilderCreationData.getEnvironment();
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck(WORKFLOW_STANDARD_PARAMS, workflowStandardParams, USER);
    notNullCheck(CURRENT_USER, workflowStandardParams.getCurrentUser(), USER);

    return Activity.builder()
        .applicationName(activityBuilderCreationData.getAppName())
        .appId(activityBuilderCreationData.getAppId())
        .commandName(activityBuilderCreationData.getCommandName())
        .type(activityBuilderCreationData.getType())
        .workflowType(executionContext.getWorkflowType())
        .workflowExecutionName(executionContext.getWorkflowExecutionName())
        .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
        .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
        .commandType(activityBuilderCreationData.getCommandType())
        .workflowExecutionId(executionContext.getWorkflowExecutionId())
        .workflowId(executionContext.getWorkflowId())
        .commandUnits(activityBuilderCreationData.getCommandUnits())
        .status(ExecutionStatus.RUNNING)
        .commandUnitType(activityBuilderCreationData.getCommandUnitType())
        .environmentId(environment.getUuid())
        .environmentName(environment.getName())
        .environmentType(environment.getEnvironmentType())
        .triggeredBy(TriggeredBy.builder()
                         .email(workflowStandardParams.getCurrentUser().getEmail())
                         .name(workflowStandardParams.getCurrentUser().getName())
                         .build());
  }

  public ExecutionResponse queueDelegateTaskForRouteUpdate(PcfRouteUpdateQueueRequestData queueRequestData) {
    Integer timeoutIntervalInMinutes = queueRequestData.getTimeoutIntervalInMinutes() == null
        ? Integer.valueOf(5)
        : queueRequestData.getTimeoutIntervalInMinutes();
    Application app = queueRequestData.getApp();
    PcfInfrastructureMapping pcfInfrastructureMapping = queueRequestData.getPcfInfrastructureMapping();
    String activityId = queueRequestData.getActivityId();

    PcfCommandRequest pcfCommandRequest = PcfCommandRouteUpdateRequest.builder()
                                              .pcfCommandType(PcfCommandType.UPDATE_ROUTE)
                                              .commandName(queueRequestData.getCommandName())
                                              .appId(app.getUuid())
                                              .accountId(app.getAccountId())
                                              .activityId(activityId)
                                              .pcfConfig(queueRequestData.getPcfConfig())
                                              .organization(pcfInfrastructureMapping.getOrganization())
                                              .space(pcfInfrastructureMapping.getSpace())
                                              .pcfRouteUpdateConfigData(queueRequestData.getRequestConfigData())
                                              .timeoutIntervalInMin(timeoutIntervalInMinutes)
                                              .build();

    PcfRouteUpdateStateExecutionData stateExecutionData =
        getRouteUpdateStateExecutionData(activityId, app.getUuid(), app.getAccountId(), pcfCommandRequest,
            queueRequestData.getCommandName(), queueRequestData.getRequestConfigData());

    DelegateTask delegateTask =
        getDelegateTask(PcfDelegateTaskCreationData.builder()
                            .waitId(queueRequestData.getActivityId())
                            .accountId(app.getAccountId())
                            .appId(app.getUuid())
                            .envId(queueRequestData.getEnvId())
                            .taskType(TaskType.PCF_COMMAND_TASK)
                            .infrastructureMappingId(pcfInfrastructureMapping.getUuid())
                            .parameters(new Object[] {pcfCommandRequest, queueRequestData.getEncryptedDataDetails()})
                            .timeout(10)
                            .build());

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(queueRequestData.getActivityId()))
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

    ActivityBuilder activityBuilder = getActivityBuilder(PcfActivityBuilderCreationData.builder()
                                                             .appName(app.getName())
                                                             .appId(app.getUuid())
                                                             .environment(env)
                                                             .type(Type.Command)
                                                             .commandName(commandName)
                                                             .executionContext(executionContext)
                                                             .commandType(stateType)
                                                             .commandUnitType(commandUnitType)
                                                             .commandUnits(emptyList())
                                                             .build());

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

  public List<String> getRouteMaps(
      String applicationManifestYmlContent, PcfInfrastructureMapping pcfInfrastructureMapping) {
    Yaml yaml = new Yaml();
    Map<String, Object> yamlMap = (Map<String, Object>) yaml.load(applicationManifestYmlContent);
    List<Map> applicationsMaps = (List<Map>) yamlMap.get(APPLICATION_YML_ELEMENT);

    if (isEmpty(applicationsMaps)) {
      throw new InvalidArgumentsException(Pair.of("Manifest", "contains no application config"));
    }

    // Always assume, 1st app is main application being deployed.
    Map application = applicationsMaps.get(0);
    Map<String, Object> applicationConfigMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    applicationConfigMap.putAll(application);

    // fetch Routes element from application config
    final List<Map<String, String>> routeMapsInYaml =
        (List<Map<String, String>>) applicationConfigMap.get(ROUTES_MANIFEST_YML_ELEMENT);

    // routes is not mentioned in Manifest
    if (isEmpty(routeMapsInYaml)) {
      List<String> infraMapRoutes = pcfInfrastructureMapping.getRouteMaps();
      // Manifest mentions no-route or route is not provided in infraMapping as well.
      if (useNoRoute(applicationConfigMap) || isEmpty(infraMapRoutes)) {
        return emptyList();
      }

      return infraMapRoutes;
    } else if (routeMapsInYaml.size() == 1) {
      Map mapForRoute = routeMapsInYaml.get(0);
      String routeValue = (String) mapForRoute.get(ROUTE_MANIFEST_YML_ELEMENT);
      List<String> routes = new ArrayList<>();
      // if manifest contains "${ROUTE_MAP}", means read from InfraMapping
      if (ROUTE_PLACEHOLDER_TOKEN_DEPRECATED.equals(routeValue)) {
        return isEmpty(pcfInfrastructureMapping.getRouteMaps()) ? emptyList() : pcfInfrastructureMapping.getRouteMaps();
      }

      // actual route value is mentioned
      routes.add(routeValue);
      return routes;
    } else {
      // more than 1 routes are mentioned, means user has mentioned multiple actual route values
      List<String> routes = new ArrayList<>();
      routeMapsInYaml.forEach(routeMap -> routes.add(routeMap.get(ROUTE_MANIFEST_YML_ELEMENT)));
      return routes;
    }
  }

  private boolean useNoRoute(Map application) {
    return application.containsKey(NO_ROUTE_MANIFEST_YML_ELEMENT)
        && (boolean) application.get(NO_ROUTE_MANIFEST_YML_ELEMENT);
  }
}
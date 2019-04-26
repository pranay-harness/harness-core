package software.wings.sm.states;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.TaskType.HELM_COMMAND_TASK;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT_SECONDS;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_NAMESPACE_PLACEHOLDER_REGEX;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.HELM_DEPLOY;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.HelmDeployContextElement;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.api.InfraMappingElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.HelmDummyCommandUnit;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest.HelmInstallCommandRequestBuilder;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmValuesFetchTaskParameters;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.HelmChartConfigHelperService;
import software.wings.service.impl.artifact.ArtifactCollectionUtil;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.Builder;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.ApplicationManifestUtils;
import software.wings.utils.KubernetesConvention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 3/25/18.
 */
@Slf4j
public class HelmDeployState extends State {
  @Inject private transient AppService appService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient ContainerDeploymentManagerHelper containerDeploymentHelper;
  @Inject private transient SettingsService settingsService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient ArtifactCollectionUtil artifactCollectionUtil;
  @Inject private transient TemplateExpressionProcessor templateExpressionProcessor;
  @Inject private transient GitConfigHelperService gitConfigHelperService;
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient ApplicationManifestUtils applicationManifestUtils;
  @Inject private transient HelmChartConfigHelperService helmChartConfigHelperService;

  @DefaultValue("10") private int steadyStateTimeout; // Minutes

  // This field is in fact representing helmReleaseName. We will change it later on
  @Getter @Setter private String helmReleaseNamePrefix;
  @Getter @Setter private GitFileConfig gitFileConfig;
  @Getter @Setter private String commandFlags;

  public static final String HELM_COMMAND_NAME = "Helm Deploy";
  private static final String DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_TAG}";
  private static final String DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_NAME}";

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public HelmDeployState(String name) {
    super(name, HELM_DEPLOY.name());
  }

  public HelmDeployState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) throws InterruptedException {
    boolean valuesInGit = false;
    boolean valuesInHelmChartRepo = false;
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();

    if (HELM_DEPLOY.name().equals(this.getStateType())) {
      appManifestMap = applicationManifestUtils.getApplicationManifests(context);
      valuesInHelmChartRepo = applicationManifestUtils.isValuesInHelmChartRepo(context);
      valuesInGit = applicationManifestUtils.isValuesInGit(appManifestMap);
    }

    Activity activity = createActivity(context, getCommandUnits(valuesInGit, valuesInHelmChartRepo));

    if (valuesInHelmChartRepo) {
      return executeHelmValuesFetchTask(context, activity.getUuid());
    }
    if (valuesInGit) {
      return executeGitTask(context, activity.getUuid(), appManifestMap);
    }

    return executeHelmTask(context, activity.getUuid(), appManifestMap);
  }

  protected List<CommandUnit> getCommandUnits(boolean valuesInGit, boolean valuesInHelmChartRepo) {
    List<CommandUnit> commandUnits = new ArrayList<>();

    if (valuesInGit || valuesInHelmChartRepo) {
      commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.FetchFiles));
    }

    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.Init));
    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.Prepare));
    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.InstallUpgrade));
    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.WaitForSteadyState));
    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.WrapUp));

    return commandUnits;
  }

  protected ImageDetails getImageDetails(ExecutionContext context, Application app, Artifact artifact) {
    return artifactCollectionUtil.fetchContainerImageDetails(artifact, app.getUuid(), context.getWorkflowExecutionId());
  }

  protected void setNewAndPrevReleaseVersion(ExecutionContext context, Application app, String releaseName,
      ContainerServiceParams containerServiceParams, HelmDeployStateExecutionData stateExecutionData,
      GitConfig gitConfig, List<EncryptedDataDetail> encryptedDataDetails, String commandFlags)
      throws InterruptedException {
    logger.info("Setting new and previous helm release version");
    int prevVersion = getPreviousReleaseVersion(app.getUuid(), app.getAccountId(), releaseName, containerServiceParams,
        gitConfig, encryptedDataDetails, commandFlags);

    stateExecutionData.setReleaseOldVersion(prevVersion);
    stateExecutionData.setReleaseNewVersion(prevVersion + 1);
  }

  private void validateChartSpecification(HelmChartSpecification chartSpec) {
    if (chartSpec == null || (isEmpty(chartSpec.getChartName()) && isEmpty(chartSpec.getChartUrl()))) {
      throw new InvalidRequestException(
          "Invalid chart specification. " + (chartSpec == null ? "Chart Specification is null" : chartSpec.toString()),
          WingsException.USER);
    }
  }

  protected HelmCommandRequest getHelmCommandRequest(ExecutionContext context,
      HelmChartSpecification helmChartSpecification, ContainerServiceParams containerServiceParams, String releaseName,
      String accountId, String appId, String activityId, ImageDetails imageDetails,
      ContainerInfrastructureMapping infrastructureMapping, String repoName, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String commandFlags, K8sDelegateManifestConfig repoConfig,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    List<String> helmValueOverridesYamlFilesEvaluated = getValuesYamlOverrides(
        context, containerServiceParams, appId, imageDetails, infrastructureMapping, appManifestMap);

    // TODO: this fix makes the previous behavior more obvious. We should review why we are overriding the value here
    steadyStateTimeout = DEFAULT_STEADY_STATE_TIMEOUT;

    HelmInstallCommandRequestBuilder helmInstallCommandRequestBuilder =
        HelmInstallCommandRequest.builder()
            .appId(appId)
            .accountId(accountId)
            .activityId(activityId)
            .commandName(HELM_COMMAND_NAME)
            .chartSpecification(helmChartSpecification)
            .releaseName(releaseName)
            .namespace(containerServiceParams.getNamespace())
            .containerServiceParams(containerServiceParams)
            .variableOverridesYamlFiles(helmValueOverridesYamlFilesEvaluated)
            .timeoutInMillis(TimeUnit.MINUTES.toMillis(steadyStateTimeout))
            .repoName(repoName)
            .gitConfig(gitConfig)
            .encryptedDataDetails(encryptedDataDetails)
            .commandFlags(commandFlags)
            .sourceRepoConfig(repoConfig);

    if (gitFileConfig != null) {
      helmInstallCommandRequestBuilder.gitFileConfig(gitFileConfig);
      helmInstallCommandRequestBuilder.gitConfig(gitConfig);
      helmInstallCommandRequestBuilder.encryptedDataDetails(encryptedDataDetails);
    }

    return helmInstallCommandRequestBuilder.build();
  }

  private String getImageName(String yamlFileContent, String imageNameTag, String domainName) {
    if (isNotEmpty(domainName)) {
      Pattern pattern = ContainerTask.compileRegexPattern(domainName);
      Matcher matcher = pattern.matcher(yamlFileContent);
      if (!matcher.find()) {
        imageNameTag = domainName + "/" + imageNameTag;
        imageNameTag = imageNameTag.replaceAll("//", "/");
      }
    }

    return imageNameTag;
  }

  protected int getPreviousReleaseVersion(String appId, String accountId, String releaseName,
      ContainerServiceParams containerServiceParams, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String commandFlags) throws InterruptedException {
    int prevVersion = 0;
    HelmReleaseHistoryCommandRequest helmReleaseHistoryCommandRequest =
        HelmReleaseHistoryCommandRequest.builder()
            .releaseName(releaseName)
            .containerServiceParams(containerServiceParams)
            .gitConfig(gitConfig)
            .encryptedDataDetails(encryptedDataDetails)
            .commandFlags(commandFlags)
            .build();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .data(TaskData.builder()
                      .taskType(HELM_COMMAND_TASK.name())
                      .parameters(new Object[] {helmReleaseHistoryCommandRequest})
                      .timeout(Long.parseLong(DEFAULT_TILLER_CONNECTION_TIMEOUT_SECONDS) * 2 * 1000)
                      .build())
            .accountId(accountId)
            .appId(appId)
            .async(false)
            .build();

    HelmCommandExecutionResponse helmCommandExecutionResponse;
    ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
    if (notifyResponseData instanceof HelmCommandExecutionResponse) {
      helmCommandExecutionResponse = (HelmCommandExecutionResponse) notifyResponseData;
    } else {
      String msg =
          " Failed to find the previous helm release version. Make sure that the helm client and tiller is installed.";
      logger.error(msg);
      throw new InvalidRequestException(msg, WingsException.USER);
    }

    if (helmCommandExecutionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
      List<ReleaseInfo> releaseInfoList =
          ((HelmReleaseHistoryCommandResponse) helmCommandExecutionResponse.getHelmCommandResponse())
              .getReleaseInfoList();
      prevVersion = isEmpty(releaseInfoList)
          ? 0
          : Integer.parseInt(releaseInfoList.get(releaseInfoList.size() - 1).getRevision());
    } else {
      String errorMsg = helmCommandExecutionResponse.getErrorMessage();
      throw new InvalidRequestException(errorMsg);
    }
    return prevVersion;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response)
      throws InterruptedException {
    HelmDeployStateExecutionData helmStateExecutionData =
        (HelmDeployStateExecutionData) context.getStateExecutionData();

    TaskType taskType = helmStateExecutionData.getCurrentTaskType();
    switch (taskType) {
      case HELM_VALUES_FETCH:
        return handleAsyncResponseForHelmFetchTask(context, response);
      case GIT_COMMAND:
        return handleAsyncResponseForGitFetchFilesTask(context, response);
      case HELM_COMMAND_TASK:
        return handleAsyncResponseForHelmTask(context, response);

      default:
        throw new WingsException("Unhandled task type " + taskType);
    }
  }

  private ExecutionResponse handleAsyncResponseForHelmFetchTask(
      ExecutionContext context, Map<String, ResponseData> response) throws InterruptedException {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String activityId = getActivityId(context);
    HelmValuesFetchTaskResponse executionResponse = (HelmValuesFetchTaskResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED.equals(executionStatus)) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return anExecutionResponse().withExecutionStatus(executionStatus).build();
    }

    if (isNotBlank(executionResponse.getValuesFileContent())) {
      HelmDeployStateExecutionData helmDeployStateExecutionData =
          (HelmDeployStateExecutionData) context.getStateExecutionData();
      helmDeployStateExecutionData.getValuesFiles().put(
          K8sValuesLocation.Service, executionResponse.getValuesFileContent());
    }

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        applicationManifestUtils.getValuesApplicationManifests(context);

    boolean valuesInGit = applicationManifestUtils.isValuesInGit(appManifestMap);
    if (valuesInGit) {
      return executeGitTask(context, activityId, appManifestMap);
    } else {
      return executeHelmTask(context, activityId, appManifestMap);
    }
  }

  public String getActivityId(ExecutionContext context) {
    return ((HelmDeployStateExecutionData) context.getStateExecutionData()).getActivityId();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext, List<CommandUnit> commandUnits) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(HELM_COMMAND_NAME)
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(commandUnits)
                                          .status(ExecutionStatus.RUNNING)
                                          .commandUnitType(CommandUnitType.HELM)
                                          .triggeredBy(TriggeredBy.builder()
                                                           .email(workflowStandardParams.getCurrentUser().getEmail())
                                                           .name(workflowStandardParams.getCurrentUser().getName())
                                                           .build());

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }
    if (instanceElement != null) {
      activityBuilder.serviceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .serviceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .serviceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .serviceInstanceId(instanceElement.getUuid())
          .hostName(instanceElement.getHost().getHostName());
    }

    Activity activity = activityBuilder.build();
    return activityService.save(activity);
  }

  public int getSteadyStateTimeout() {
    return steadyStateTimeout;
  }

  public void setSteadyStateTimeout(int steadyStateTimeout) {
    this.steadyStateTimeout = steadyStateTimeout;
  }

  private String getRepoName(String appName, String serviceName) {
    return KubernetesConvention.normalize(appName) + "-" + KubernetesConvention.normalize(serviceName);
  }

  private void evaluateHelmChartSpecificationExpression(
      ExecutionContext context, HelmChartSpecification helmChartSpec) {
    if (helmChartSpec == null) {
      return;
    }

    if (isNotBlank(helmChartSpec.getChartUrl())) {
      helmChartSpec.setChartUrl(context.renderExpression(helmChartSpec.getChartUrl()));
    }

    if (isNotBlank(helmChartSpec.getChartVersion())) {
      helmChartSpec.setChartVersion(context.renderExpression(helmChartSpec.getChartVersion()));
    }

    if (isNotBlank(helmChartSpec.getChartName())) {
      helmChartSpec.setChartName(context.renderExpression(helmChartSpec.getChartName()));
    }
  }

  private String obtainHelmReleaseNamePrefix(ExecutionContext context) {
    if (getStateType().equals(HELM_DEPLOY.name())) {
      if (isBlank(getHelmReleaseNamePrefix())) {
        throw new InvalidRequestException("Helm release name cannot be empty");
      }
      return KubernetesConvention.normalize(context.renderExpression(getHelmReleaseNamePrefix()));
    } else {
      HelmDeployContextElement contextElement = context.getContextElement(ContextElementType.HELM_DEPLOY);
      if (contextElement == null || isBlank(contextElement.getReleaseName())) {
        throw new InvalidRequestException("Helm rollback is not possible without deployment");
      }
      return contextElement.getReleaseName();
    }
  }

  private String obtainCommandFlags(ExecutionContext context) {
    if (getStateType().equals(HELM_DEPLOY.name())) {
      String commandFlags = getCommandFlags();

      if (isNotBlank(commandFlags)) {
        commandFlags = context.renderExpression(commandFlags);
      }

      return commandFlags;
    } else {
      HelmDeployContextElement contextElement = context.getContextElement(ContextElementType.HELM_DEPLOY);
      if (contextElement == null) {
        return null;
      }

      return contextElement.getCommandFlags();
    }
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isBlank(getHelmReleaseNamePrefix())) {
      invalidFields.put("Helm release name prefix", "Helm release name prefix must not be blank");
    }
    if (gitFileConfig != null && isNotBlank(gitFileConfig.getConnectorId())) {
      if (isBlank(gitFileConfig.getFilePath())) {
        invalidFields.put("File path", "File path must not be blank if git connector is selected");
      }

      if (isBlank(gitFileConfig.getBranch()) && isBlank(gitFileConfig.getCommitId())) {
        invalidFields.put("Branch or commit id", "Branch or commit id must not be blank if git connector is selected");
      }
    }

    return invalidFields;
  }

  private void evaluateGitFileConfig(ExecutionContext context) {
    if (isNotBlank(gitFileConfig.getCommitId())) {
      gitFileConfig.setCommitId(context.renderExpression(gitFileConfig.getCommitId()));
    }

    if (isNotBlank(gitFileConfig.getBranch())) {
      gitFileConfig.setBranch(context.renderExpression(gitFileConfig.getBranch()));
    }

    if (isNotBlank(gitFileConfig.getFilePath())) {
      gitFileConfig.setFilePath(context.renderExpression(gitFileConfig.getFilePath()));
    }
  }

  private List<EncryptedDataDetail> fetchEncryptedDataDetail(ExecutionContext context, GitConfig gitConfig) {
    if (gitConfig == null) {
      return null;
    }

    return secretManager.getEncryptionDetails(gitConfig, context.getAppId(), context.getWorkflowExecutionId());
  }

  private HelmCommandExecutionResponse fetchHelmCommandExecutionResponse(ResponseData notifyResponseData) {
    if (!(notifyResponseData instanceof HelmCommandExecutionResponse)) {
      String msg = "Delegate returned error response. Could not convert delegate response to helm response. ";

      if (notifyResponseData instanceof RemoteMethodReturnValueData) {
        msg += notifyResponseData.toString();
      }
      throw new InvalidRequestException(msg, WingsException.USER);
    }

    return (HelmCommandExecutionResponse) notifyResponseData;
  }

  public void updateHelmReleaseNameInInfraMappingElement(ExecutionContext context, String helmReleaseName) {
    if (getStateType().equals(HELM_DEPLOY.name())) {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      if (workflowStandardParams == null) {
        return;
      }

      InfraMappingElement infraMappingElement = workflowStandardParams.getInfraMappingElement(context);
      if (infraMappingElement != null && infraMappingElement.getHelm() != null) {
        infraMappingElement.getHelm().setReleaseName(helmReleaseName);
      }
    }
  }

  private ExecutionResponse executeHelmTask(ExecutionContext context, String activityId,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap) throws InterruptedException {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceElement.getUuid());

    ContainerInfrastructureMapping containerInfraMapping =
        (ContainerInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    String releaseName = obtainHelmReleaseNamePrefix(context);
    updateHelmReleaseNameInInfraMappingElement(context, releaseName);

    String commandFlags = obtainCommandFlags(context);

    ContainerServiceParams containerServiceParams =
        containerDeploymentHelper.getContainerServiceParams(containerInfraMapping, releaseName, context);

    HelmChartSpecification helmChartSpecification =
        serviceResourceService.getHelmChartSpecification(context.getAppId(), serviceElement.getUuid());

    K8sDelegateManifestConfig repoConfig = null;
    ApplicationManifest appManifest = applicationManifestService.getAppManifest(
        app.getUuid(), null, serviceElement.getUuid(), AppManifestKind.K8S_MANIFEST);
    if (appManifest != null) {
      switch (appManifest.getStoreType()) {
        case HelmSourceRepo:
          GitFileConfig sourceRepoGitFileConfig = appManifest.getGitFileConfig();
          GitConfig sourceRepoGitConfig =
              settingsService.fetchGitConfigFromConnectorId(sourceRepoGitFileConfig.getConnectorId());
          repoConfig = K8sDelegateManifestConfig.builder()
                           .gitFileConfig(sourceRepoGitFileConfig)
                           .gitConfig(sourceRepoGitConfig)
                           .encryptedDataDetails(fetchEncryptedDataDetail(context, sourceRepoGitConfig))
                           .manifestStoreTypes(StoreType.HelmSourceRepo)
                           .build();

          break;
        case HelmChartRepo:
          HelmChartConfigParams helmChartConfigTaskParams =
              helmChartConfigHelperService.getHelmChartConfigTaskParams(context, appManifest);
          repoConfig = K8sDelegateManifestConfig.builder()
                           .helmChartConfigParams(helmChartConfigTaskParams)
                           .manifestStoreTypes(StoreType.HelmChartRepo)
                           .build();
          break;
        default:
          throw new WingsException("Unsupported store type: " + appManifest.getStoreType());
      }
    }

    if (StateType.HELM_DEPLOY.name().equals(getStateType())) {
      if ((gitFileConfig == null || gitFileConfig.getConnectorId() == null) && repoConfig == null) {
        validateChartSpecification(helmChartSpecification);
      }
      evaluateHelmChartSpecificationExpression(context, helmChartSpecification);
    }

    HelmDeployStateExecutionData stateExecutionData = HelmDeployStateExecutionData.builder()
                                                          .activityId(activityId)
                                                          .releaseName(releaseName)
                                                          .namespace(containerServiceParams.getNamespace())
                                                          .commandFlags(commandFlags)
                                                          .currentTaskType(HELM_COMMAND_TASK)
                                                          .build();

    if (helmChartSpecification != null) {
      stateExecutionData.setChartName(helmChartSpecification.getChartName());
      stateExecutionData.setChartRepositoryUrl(helmChartSpecification.getChartUrl());
      stateExecutionData.setChartVersion(helmChartSpecification.getChartVersion());
    }

    ImageDetails imageDetails = null;
    if (artifact != null) {
      imageDetails = getImageDetails(context, app, artifact);
    }

    String repoName = getRepoName(app.getName(), serviceElement.getName());

    List<EncryptedDataDetail> encryptedDataDetails = null;
    GitConfig gitConfig = null;
    if (gitFileConfig != null) {
      evaluateGitFileConfig(context);
      List<TemplateExpression> templateExpressions = getTemplateExpressions();
      if (isNotEmpty(templateExpressions)) {
        TemplateExpression configIdExpression =
            templateExpressionProcessor.getTemplateExpression(templateExpressions, "connectorId");
        SettingAttribute settingAttribute = templateExpressionProcessor.resolveSettingAttributeByNameOrId(
            context, configIdExpression, SettingVariableTypes.GIT);
        SettingValue settingValue = settingAttribute.getValue();
        if (!(settingValue instanceof GitConfig)) {
          throw new InvalidRequestException("Git connector not found", USER);
        }
        gitConfig = (GitConfig) settingValue;
        gitConfigHelperService.setSshKeySettingAttributeIfNeeded(gitConfig);
      } else {
        gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
      }
      encryptedDataDetails = fetchEncryptedDataDetail(context, gitConfig);
    }

    setNewAndPrevReleaseVersion(context, app, releaseName, containerServiceParams, stateExecutionData, gitConfig,
        encryptedDataDetails, commandFlags);
    HelmCommandRequest commandRequest = getHelmCommandRequest(context, helmChartSpecification, containerServiceParams,
        releaseName, app.getAccountId(), app.getUuid(), activityId, imageDetails, containerInfraMapping, repoName,
        gitConfig, encryptedDataDetails, commandFlags, repoConfig, appManifestMap);

    delegateService.queueTask(DelegateTask.builder()
                                  .async(true)
                                  .accountId(app.getAccountId())
                                  .appId(app.getUuid())
                                  .waitId(activityId)
                                  .data(TaskData.builder()
                                            .taskType(HELM_COMMAND_TASK.name())
                                            .parameters(new Object[] {commandRequest})
                                            .timeout(TimeUnit.HOURS.toMillis(1))
                                            .build())
                                  .envId(env.getUuid())
                                  .infrastructureMappingId(containerInfraMapping.getUuid())
                                  .build());
    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(singletonList(activityId))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  private ExecutionResponse executeGitTask(
      ExecutionContext context, String activityId, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    Application app = appService.get(context.getAppId());

    GitFetchFilesTaskParams fetchFilesTaskParams =
        applicationManifestUtils.createGitFetchFilesTaskParams(context, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(activityId);
    applicationManifestUtils.setValuesPathInGitFetchFilesTaskParams(fetchFilesTaskParams);

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .async(true)
                                    .waitId(waitId)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.GIT_FETCH_FILES_TASK.name())
                                              .parameters(new Object[] {fetchFilesTaskParams})
                                              .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                                              .build())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(waitId))
        .withStateExecutionData(HelmDeployStateExecutionData.builder()
                                    .activityId(activityId)
                                    .commandName(HELM_COMMAND_NAME)
                                    .currentTaskType(TaskType.GIT_COMMAND)
                                    .appManifestMap(appManifestMap)
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  protected ExecutionResponse handleAsyncResponseForHelmTask(
      ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    HelmCommandExecutionResponse executionResponse =
        fetchHelmCommandExecutionResponse(response.values().iterator().next());
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    HelmDeployStateExecutionData stateExecutionData = (HelmDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    Builder executionResponseBuilder = Builder.anExecutionResponse()
                                           .withExecutionStatus(executionStatus)
                                           .withErrorMessage(executionResponse.getErrorMessage())
                                           .withStateExecutionData(stateExecutionData);

    if (executionResponse.getHelmCommandResponse() == null) {
      logger.info("Helm command task failed with status " + executionResponse.getCommandExecutionStatus().toString()
          + " with error message " + executionResponse.getErrorMessage());

      return executionResponseBuilder.build();
    }

    if (CommandExecutionStatus.SUCCESS.equals(executionResponse.getHelmCommandResponse().getCommandExecutionStatus())) {
      HelmInstallCommandResponse helmInstallCommandResponse =
          (HelmInstallCommandResponse) executionResponse.getHelmCommandResponse();

      if (helmInstallCommandResponse != null) {
        List<InstanceStatusSummary> instanceStatusSummaries = containerDeploymentHelper.getInstanceStatusSummaries(
            context, helmInstallCommandResponse.getContainerInfoList());
        stateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);

        List<InstanceElement> instanceElements =
            instanceStatusSummaries.stream().map(InstanceStatusSummary::getInstanceElement).collect(toList());
        InstanceElementListParam instanceElementListParam =
            InstanceElementListParamBuilder.anInstanceElementListParam().withInstanceElements(instanceElements).build();

        executionResponseBuilder.addContextElement(instanceElementListParam);
        executionResponseBuilder.addNotifyElement(instanceElementListParam);
      }
    } else {
      logger.info("Got helm execution response with status "
          + executionResponse.getHelmCommandResponse().getCommandExecutionStatus().toString() + " with output "
          + executionResponse.getHelmCommandResponse().getOutput());
    }

    return executionResponseBuilder.build();
  }

  private ExecutionResponse handleAsyncResponseForGitFetchFilesTask(
      ExecutionContext context, Map<String, ResponseData> response) throws InterruptedException {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String activityId = getActivityId(context);

    GitCommandExecutionResponse executionResponse = (GitCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getGitCommandStatus().equals(GitCommandStatus.SUCCESS)
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED.equals(executionStatus)) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return anExecutionResponse().withExecutionStatus(executionStatus).build();
    }

    Map<K8sValuesLocation, String> valuesFiles =
        applicationManifestUtils.getValuesFilesFromGitFetchFilesResponse(executionResponse);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) context.getStateExecutionData();
    helmDeployStateExecutionData.getValuesFiles().putAll(valuesFiles);

    return executeHelmTask(context, activityId, helmDeployStateExecutionData.getAppManifestMap());
  }

  private List<String> getValuesYamlOverrides(ExecutionContext context, ContainerServiceParams containerServiceParams,
      String appId, ImageDetails imageDetails, ContainerInfrastructureMapping infrastructureMapping,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();

    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) context.getStateExecutionData();
    if (helmDeployStateExecutionData != null) {
      valuesFiles.putAll(helmDeployStateExecutionData.getValuesFiles());
    }

    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);

    // ToDo anshul - Remove this piece of code once the values yaml in service has been migrated to ManifestFiles format
    if (!appManifestMap.containsKey(K8sValuesLocation.ServiceOverride)) {
      ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, infrastructureMapping.getServiceTemplateId());
      if (serviceTemplate != null) {
        Service service = serviceResourceService.get(appId, serviceTemplate.getServiceId(), false);
        if (service != null && isNotBlank(service.getHelmValueYaml())) {
          valuesFiles.put(K8sValuesLocation.ServiceOverride, service.getHelmValueYaml());
        }
      }
    }

    logger.info("Found Values at following sources: " + valuesFiles.keySet());
    List<String> helmValueOverridesYamlFiles = getOrderedValuesYamlList(valuesFiles);

    List<String> helmValueOverridesYamlFilesEvaluated = null;
    if (isNotEmpty(helmValueOverridesYamlFiles)) {
      helmValueOverridesYamlFilesEvaluated =
          helmValueOverridesYamlFiles.stream()
              .map(yamlFileContent -> {
                if (imageDetails != null) {
                  yamlFileContent =
                      yamlFileContent.replaceAll(DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX, imageDetails.getTag())
                          .replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX,
                              getImageName(yamlFileContent, imageDetails.getName(), imageDetails.getDomainName()));
                }
                yamlFileContent =
                    yamlFileContent.replaceAll(HELM_NAMESPACE_PLACEHOLDER_REGEX, containerServiceParams.getNamespace());
                return yamlFileContent;
              })
              .map(context::renderExpression)
              .collect(Collectors.toList());
    }

    return helmValueOverridesYamlFilesEvaluated;
  }

  private List<String> getOrderedValuesYamlList(Map<K8sValuesLocation, String> valuesFiles) {
    List<String> valuesList = new ArrayList<>();

    if (valuesFiles.containsKey(K8sValuesLocation.Service)) {
      valuesList.add(valuesFiles.get(K8sValuesLocation.Service));
    }

    if (valuesFiles.containsKey(K8sValuesLocation.ServiceOverride)) {
      valuesList.add(valuesFiles.get(K8sValuesLocation.ServiceOverride));
    }

    if (valuesFiles.containsKey(K8sValuesLocation.EnvironmentGlobal)) {
      valuesList.add(valuesFiles.get(K8sValuesLocation.EnvironmentGlobal));
    }

    if (valuesFiles.containsKey(K8sValuesLocation.Environment)) {
      valuesList.add(valuesFiles.get(K8sValuesLocation.Environment));
    }

    return valuesList;
  }

  public ExecutionResponse executeHelmValuesFetchTask(ExecutionContext context, String activityId) {
    Application app = appService.get(context.getAppId());
    HelmValuesFetchTaskParameters helmValuesFetchTaskParameters = getHelmValuesFetchTaskParameters(context, activityId);

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .waitId(waitId)
                                    .async(true)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.HELM_VALUES_FETCH.name())
                                              .parameters(new Object[] {helmValuesFetchTaskParameters})
                                              .timeout(TimeUnit.MINUTES.toMillis(10))
                                              .build())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(waitId))
        .withStateExecutionData(HelmDeployStateExecutionData.builder()
                                    .activityId(activityId)
                                    .commandName(HELM_COMMAND_NAME)
                                    .currentTaskType(TaskType.HELM_VALUES_FETCH)
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private HelmValuesFetchTaskParameters getHelmValuesFetchTaskParameters(ExecutionContext context, String activityId) {
    ApplicationManifest applicationManifest = applicationManifestUtils.getApplicationManifestForService(context);
    if (!StoreType.HelmChartRepo.equals(applicationManifest.getStoreType())) {
      return null;
    }

    return HelmValuesFetchTaskParameters.builder()
        .accountId(context.getAccountId())
        .appId(context.getAppId())
        .activityId(activityId)
        .helmChartConfigTaskParams(
            helmChartConfigHelperService.getHelmChartConfigTaskParams(context, applicationManifest))
        .workflowExecutionId(context.getWorkflowExecutionId())
        .build();
  }
}

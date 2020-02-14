package software.wings.sm.states;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.TaskType.HELM_COMMAND_TASK;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_NAMESPACE_PLACEHOLDER_REGEX;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;
import static software.wings.sm.StateType.HELM_DEPLOY;

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
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.HelmDeployContextElement;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.api.InfraMappingElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmExecutionSummary;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
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
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.helm.HelmHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest.HelmInstallCommandRequestBuilder;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmValuesFetchTaskParameters;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.HelmChartConfigHelperService;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.k8s.K8sStateHelper;
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
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private DelegateService delegateService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ActivityService activityService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentHelper;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private TemplateExpressionProcessor templateExpressionProcessor;
  @Inject private GitConfigHelperService gitConfigHelperService;
  @Inject private GitFileConfigHelperService gitFileConfigHelperService;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private ApplicationManifestUtils applicationManifestUtils;
  @Inject private HelmChartConfigHelperService helmChartConfigHelperService;
  @Inject private ServiceTemplateHelper serviceTemplateHelper;
  @Inject private K8sStateHelper k8sStateHelper;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private HelmHelper helmHelper;
  @Inject private FeatureFlagService featureFlagService;

  @DefaultValue("10") private int steadyStateTimeout; // Minutes

  // This field is in fact representing helmReleaseName. We will change it later on
  @Getter @Setter private String helmReleaseNamePrefix;
  @Getter @Setter private GitFileConfig gitFileConfig;
  @Getter @Setter private String commandFlags;

  public static final String HELM_COMMAND_NAME = "Helm Deploy";
  private static final String DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_TAG}";
  private static final String DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_NAME}";

  // Workaround for CDP-10845
  private static final int minTimeoutInMs = 60000;

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
      appManifestMap = applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES);
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
    return artifactCollectionUtils.fetchContainerImageDetails(artifact, context.getWorkflowExecutionId());
  }

  protected void setNewAndPrevReleaseVersion(ExecutionContext context, Application app, String releaseName,
      ContainerServiceParams containerServiceParams, HelmDeployStateExecutionData stateExecutionData,
      GitConfig gitConfig, List<EncryptedDataDetail> encryptedDataDetails, String commandFlags, HelmVersion helmVersion)
      throws InterruptedException {
    logger.info("Setting new and previous helm release version");
    int prevVersion = getPreviousReleaseVersion(
        app, releaseName, containerServiceParams, gitConfig, encryptedDataDetails, commandFlags, helmVersion);

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

  protected long getTimeout(long steadyStateTimeout) {
    // Temporary workaround for CDP-10845
    return steadyStateTimeout > minTimeoutInMs ? steadyStateTimeout : TimeUnit.MINUTES.toMillis(steadyStateTimeout);
  }

  protected HelmCommandRequest getHelmCommandRequest(ExecutionContext context,
      HelmChartSpecification helmChartSpecification, ContainerServiceParams containerServiceParams, String releaseName,
      String accountId, String appId, String activityId, ImageDetails imageDetails,
      ContainerInfrastructureMapping infrastructureMapping, String repoName, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String commandFlags, K8sDelegateManifestConfig repoConfig,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, HelmVersion helmVersion) {
    List<String> helmValueOverridesYamlFilesEvaluated = getValuesYamlOverrides(
        context, containerServiceParams, appId, imageDetails, infrastructureMapping, appManifestMap);

    steadyStateTimeout = steadyStateTimeout > 0 ? steadyStateTimeout : DEFAULT_STEADY_STATE_TIMEOUT;

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
            .timeoutInMillis(getTimeout(steadyStateTimeout))
            .repoName(repoName)
            .gitConfig(gitConfig)
            .encryptedDataDetails(encryptedDataDetails)
            .commandFlags(commandFlags)
            .sourceRepoConfig(repoConfig)
            .helmVersion(helmVersion);

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

  protected int getPreviousReleaseVersion(Application app, String releaseName,
      ContainerServiceParams containerServiceParams, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String commandFlags, HelmVersion helmVersion)
      throws InterruptedException {
    int prevVersion = 0;
    HelmReleaseHistoryCommandRequest helmReleaseHistoryCommandRequest =
        HelmReleaseHistoryCommandRequest.builder()
            .releaseName(releaseName)
            .containerServiceParams(containerServiceParams)
            .gitConfig(gitConfig)
            .encryptedDataDetails(encryptedDataDetails)
            .commandFlags(commandFlags)
            .helmVersion(helmVersion)
            .build();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .data(TaskData.builder()
                                              .taskType(HELM_COMMAND_TASK.name())
                                              .parameters(new Object[] {helmReleaseHistoryCommandRequest})
                                              .timeout(DEFAULT_TILLER_CONNECTION_TIMEOUT_MILLIS * 2)
                                              .build())
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .async(false)
                                    .build();

    HelmCommandExecutionResponse helmCommandExecutionResponse;
    ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
    if (notifyResponseData instanceof HelmCommandExecutionResponse) {
      helmCommandExecutionResponse = (HelmCommandExecutionResponse) notifyResponseData;
    } else {
      StringBuilder builder = new StringBuilder(256);
      builder.append("Failed to find the previous helm release version. ");
      if (helmVersion == HelmVersion.V3) {
        builder.append("Make sure Helm 3 is installed");
      } else {
        builder.append("Make sure that the helm client and tiller is installed");
      }

      if (gitConfig != null) {
        builder.append(" and delegate has git connectivity");
      }

      SettingValue value = containerServiceParams.getSettingAttribute().getValue();
      if (value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate()) {
        builder.append(" and correct delegate name is selected in the cloud provider");
      }

      logger.info(builder.toString());
      throw new InvalidRequestException(builder.toString(), WingsException.USER);
    }

    if (helmCommandExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
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
    String activityId = obtainActivityId(context);
    HelmValuesFetchTaskResponse executionResponse = (HelmValuesFetchTaskResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED == executionStatus) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .errorMessage(executionResponse.getErrorMessage())
          .build();
    }

    if (isNotBlank(executionResponse.getValuesFileContent())) {
      HelmDeployStateExecutionData helmDeployStateExecutionData =
          (HelmDeployStateExecutionData) context.getStateExecutionData();
      helmDeployStateExecutionData.getValuesFiles().put(
          K8sValuesLocation.Service, executionResponse.getValuesFileContent());
    }

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        applicationManifestUtils.getOverrideApplicationManifests(context, AppManifestKind.VALUES);

    boolean valuesInGit = applicationManifestUtils.isValuesInGit(appManifestMap);
    if (valuesInGit) {
      return executeGitTask(context, activityId, appManifestMap);
    } else {
      return executeHelmTask(context, activityId, appManifestMap);
    }
  }

  public String obtainActivityId(ExecutionContext context) {
    return ((HelmDeployStateExecutionData) context.getStateExecutionData()).getActivityId();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext, List<CommandUnit> commandUnits) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    notNullCheck("Application", app);
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
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
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
        throw new InvalidRequestException("Helm rollback is not possible without deployment", USER);
      }
      return contextElement.getReleaseName();
    }
  }

  private String obtainCommandFlags(ExecutionContext context) {
    if (getStateType().equals(HELM_DEPLOY.name())) {
      String cmdFlags = getCommandFlags();

      if (isNotBlank(cmdFlags)) {
        cmdFlags = context.renderExpression(cmdFlags);
      }

      return cmdFlags;
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

      InfraMappingElement infraMappingElement = context.fetchInfraMappingElement();
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
    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceElement.getUuid());

    ContainerInfrastructureMapping containerInfraMapping =
        (ContainerInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    String releaseName = obtainHelmReleaseNamePrefix(context);

    HelmVersion helmVersion = getHelmVersionWithDefault(app, serviceElement);
    updateHelmReleaseNameInInfraMappingElement(context, releaseName);

    String cmdFlags = obtainCommandFlags(context);

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
          GitFileConfig sourceRepoGitFileConfig =
              gitFileConfigHelperService.renderGitFileConfig(context, appManifest.getGitFileConfig());
          GitConfig sourceRepoGitConfig =
              settingsService.fetchGitConfigFromConnectorId(sourceRepoGitFileConfig.getConnectorId());
          gitConfigHelperService.renderGitConfig(context, sourceRepoGitConfig);
          repoConfig = K8sDelegateManifestConfig.builder()
                           .gitFileConfig(sourceRepoGitFileConfig)
                           .gitConfig(sourceRepoGitConfig)
                           .encryptedDataDetails(fetchEncryptedDataDetail(context, sourceRepoGitConfig))
                           .manifestStoreTypes(StoreType.HelmSourceRepo)
                           .build();

          break;
        case HelmChartRepo:
          if (appManifest.getHelmChartConfig() == null) {
            helmChartSpecification = null;
          } else if (isBlank(appManifest.getHelmChartConfig().getConnectorId())) {
            if (helmChartSpecification == null) {
              helmChartSpecification = HelmChartSpecification.builder().build();
            }
            helmChartSpecification.setChartVersion(appManifest.getHelmChartConfig().getChartVersion());
            helmChartSpecification.setChartUrl(appManifest.getHelmChartConfig().getChartUrl());
            helmChartSpecification.setChartName(appManifest.getHelmChartConfig().getChartName());
          } else {
            HelmChartConfigParams helmChartConfigTaskParams =
                helmChartConfigHelperService.getHelmChartConfigTaskParams(context, appManifest);
            repoConfig = K8sDelegateManifestConfig.builder()
                             .helmChartConfigParams(helmChartConfigTaskParams)
                             .manifestStoreTypes(HelmChartRepo)
                             .build();
          }
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
                                                          .commandFlags(cmdFlags)
                                                          .currentTaskType(HELM_COMMAND_TASK)
                                                          .build();

    setHelmExecutionSummary(context, releaseName, helmChartSpecification, repoConfig);

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
        encryptedDataDetails, cmdFlags, helmVersion);
    HelmCommandRequest commandRequest = getHelmCommandRequest(context, helmChartSpecification, containerServiceParams,
        releaseName, app.getAccountId(), app.getUuid(), activityId, imageDetails, containerInfraMapping, repoName,
        gitConfig, encryptedDataDetails, cmdFlags, repoConfig, appManifestMap, helmVersion);

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
    return ExecutionResponse.builder()
        .correlationIds(singletonList(activityId))
        .stateExecutionData(stateExecutionData)
        .async(true)
        .build();
  }

  private HelmVersion getHelmVersionWithDefault(Application app, ServiceElement serviceElement) {
    return serviceResourceService.getHelmVersionWithDefault(app.getUuid(), serviceElement.getUuid());
  }

  private ExecutionResponse executeGitTask(
      ExecutionContext context, String activityId, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    Application app = appService.get(context.getAppId());

    GitFetchFilesTaskParams fetchFilesTaskParams =
        applicationManifestUtils.createGitFetchFilesTaskParams(context, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(activityId);
    fetchFilesTaskParams.setFinalState(true);
    fetchFilesTaskParams.setAppManifestKind(AppManifestKind.VALUES);
    applicationManifestUtils.setValuesPathInGitFetchFilesTaskParams(fetchFilesTaskParams);

    String waitId = generateUuid();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .envId(k8sStateHelper.getEnvIdFromExecutionContext(context))
            .infrastructureMappingId(k8sStateHelper.getContainerInfrastructureMappingId(context))
            .async(true)
            .waitId(waitId)
            .data(TaskData.builder()
                      .taskType(TaskType.GIT_FETCH_FILES_TASK.name())
                      .parameters(new Object[] {fetchFilesTaskParams})
                      .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                      .build())
            .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    HelmDeployStateExecutionData stateExecutionData = (HelmDeployStateExecutionData) context.getStateExecutionData();
    if (stateExecutionData != null) {
      valuesFiles.putAll(stateExecutionData.getValuesFiles());
    }

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Arrays.asList(waitId))
        .stateExecutionData(HelmDeployStateExecutionData.builder()
                                .activityId(activityId)
                                .commandName(HELM_COMMAND_NAME)
                                .currentTaskType(TaskType.GIT_COMMAND)
                                .appManifestMap(appManifestMap)
                                .valuesFiles(valuesFiles)
                                .build())
        .delegateTaskId(delegateTaskId)
        .build();
  }

  protected ExecutionResponse handleAsyncResponseForHelmTask(
      ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    HelmCommandExecutionResponse executionResponse =
        fetchHelmCommandExecutionResponse(response.values().iterator().next());
    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    HelmDeployStateExecutionData stateExecutionData = (HelmDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder()
                                                            .executionStatus(executionStatus)
                                                            .errorMessage(executionResponse.getErrorMessage())
                                                            .stateExecutionData(stateExecutionData);

    HelmCommandResponse helmCommandResponse = executionResponse.getHelmCommandResponse();
    if (helmCommandResponse == null) {
      logger.info("Helm command task failed with status " + executionResponse.getCommandExecutionStatus().toString()
          + " with error message " + executionResponse.getErrorMessage());

      return executionResponseBuilder.build();
    }

    updateHelmExecutionSummary(context, helmCommandResponse);

    if (CommandExecutionStatus.SUCCESS == helmCommandResponse.getCommandExecutionStatus()) {
      HelmInstallCommandResponse helmInstallCommandResponse = (HelmInstallCommandResponse) helmCommandResponse;

      List<InstanceStatusSummary> instanceStatusSummaries = containerDeploymentHelper.getInstanceStatusSummaries(
          context, helmInstallCommandResponse.getContainerInfoList());
      stateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);

      List<InstanceElement> instanceElements =
          instanceStatusSummaries.stream().map(InstanceStatusSummary::getInstanceElement).collect(toList());
      InstanceElementListParam instanceElementListParam =
          InstanceElementListParam.builder().instanceElements(instanceElements).build();

      executionResponseBuilder.contextElement(instanceElementListParam);
      executionResponseBuilder.notifyElement(instanceElementListParam);
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
    String activityId = obtainActivityId(context);

    GitCommandExecutionResponse executionResponse = (GitCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getGitCommandStatus() == GitCommandStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED == executionStatus) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return ExecutionResponse.builder().executionStatus(executionStatus).build();
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

    logger.info("Found Values at following sources: " + valuesFiles.keySet());
    List<String> helmValueOverridesYamlFiles = getOrderedValuesYamlList(valuesFiles);

    List<String> helmValueOverridesYamlFilesEvaluated = null;
    if (isNotEmpty(helmValueOverridesYamlFiles)) {
      helmValueOverridesYamlFilesEvaluated =
          helmValueOverridesYamlFiles.stream()
              .map(yamlFileContent -> {
                if (imageDetails != null) {
                  if (isNotBlank(imageDetails.getTag())) {
                    yamlFileContent =
                        yamlFileContent.replaceAll(DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX, imageDetails.getTag());
                  }

                  if (isNotBlank(imageDetails.getName())) {
                    yamlFileContent = yamlFileContent.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX,
                        getImageName(yamlFileContent, imageDetails.getName(), imageDetails.getDomainName()));
                  }
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

  List<String> getOrderedValuesYamlList(Map<K8sValuesLocation, String> valuesFiles) {
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
    HelmValuesFetchTaskParameters helmValuesFetchTaskParameters =
        getHelmValuesFetchTaskParameters(context, app.getUuid(), activityId);

    String waitId = generateUuid();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .envId(k8sStateHelper.getEnvIdFromExecutionContext(context))
            .infrastructureMappingId(k8sStateHelper.getContainerInfrastructureMappingId(context))
            .waitId(waitId)
            .async(true)
            .data(TaskData.builder()
                      .taskType(TaskType.HELM_VALUES_FETCH.name())
                      .parameters(new Object[] {helmValuesFetchTaskParameters})
                      .timeout(TimeUnit.MINUTES.toMillis(10))
                      .build())
            .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Arrays.asList(waitId))
        .stateExecutionData(HelmDeployStateExecutionData.builder()
                                .activityId(activityId)
                                .commandName(HELM_COMMAND_NAME)
                                .currentTaskType(TaskType.HELM_VALUES_FETCH)
                                .build())
        .delegateTaskId(delegateTaskId)
        .build();
  }

  private HelmValuesFetchTaskParameters getHelmValuesFetchTaskParameters(
      ExecutionContext context, String appId, String activityId) {
    ContainerInfrastructureMapping containerInfraMapping =
        (ContainerInfrastructureMapping) infrastructureMappingService.get(appId, context.fetchInfraMappingId());

    String releaseName = obtainHelmReleaseNamePrefix(context);
    ContainerServiceParams containerServiceParams =
        containerDeploymentHelper.getContainerServiceParams(containerInfraMapping, releaseName, context);

    String evaluatedCommandFlags = obtainCommandFlags(context);

    boolean isBindTaskFeatureSet = false;
    if (featureFlagService.isEnabled(FeatureName.BIND_FETCH_FILES_TASK_TO_DELEGATE, context.getAccountId())) {
      isBindTaskFeatureSet = true;
    }

    HelmValuesFetchTaskParameters helmValuesFetchTaskParameters =
        HelmValuesFetchTaskParameters.builder()
            .accountId(context.getAccountId())
            .appId(context.getAppId())
            .activityId(activityId)
            .helmCommandFlags(evaluatedCommandFlags)
            .containerServiceParams(containerServiceParams)
            .workflowExecutionId(context.getWorkflowExecutionId())
            .isBindTaskFeatureSet(isBindTaskFeatureSet)
            .build();

    ApplicationManifest applicationManifest = applicationManifestUtils.getApplicationManifestForService(context);
    if (applicationManifest == null || HelmChartRepo != applicationManifest.getStoreType()) {
      return helmValuesFetchTaskParameters;
    }

    helmValuesFetchTaskParameters.setHelmChartConfigTaskParams(
        helmChartConfigHelperService.getHelmChartConfigTaskParams(context, applicationManifest));
    return helmValuesFetchTaskParameters;
  }

  private void setHelmExecutionSummary(ExecutionContext context, String releaseName,
      HelmChartSpecification helmChartSpec, K8sDelegateManifestConfig repoConfig) {
    try {
      if (!HELM_DEPLOY.name().equals(this.getStateType())) {
        return;
      }

      HelmExecutionSummary summary = helmHelper.prepareHelmExecutionSummary(releaseName, helmChartSpec, repoConfig);
      workflowExecutionService.refreshHelmExecutionSummary(context.getWorkflowExecutionId(), summary);
    } catch (Exception ex) {
      logger.info("Exception while setting helm execution summary", ex);
    }
  }

  private void updateHelmExecutionSummary(ExecutionContext context, HelmCommandResponse helmCommandResponse) {
    try {
      if (HELM_DEPLOY.name().equals(this.getStateType()) && helmCommandResponse instanceof HelmInstallCommandResponse) {
        HelmInstallCommandResponse helmInstallCommandResponse = (HelmInstallCommandResponse) helmCommandResponse;
        HelmChartInfo helmChartInfo = helmInstallCommandResponse.getHelmChartInfo();
        if (helmChartInfo == null) {
          return;
        }

        WorkflowExecution workflowExecution =
            workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
        if (workflowExecution == null) {
          return;
        }

        HelmExecutionSummary summary = workflowExecution.getHelmExecutionSummary();
        if (summary.getHelmChartInfo() == null) {
          summary.setHelmChartInfo(HelmChartInfo.builder().build());
        }

        if (isNotBlank(helmChartInfo.getName())) {
          summary.getHelmChartInfo().setName(helmChartInfo.getName());
        }

        if (isNotBlank(helmChartInfo.getVersion())) {
          summary.getHelmChartInfo().setVersion(helmChartInfo.getVersion());
        }

        if (isNotBlank(helmChartInfo.getRepoUrl())) {
          summary.getHelmChartInfo().setRepoUrl(helmChartInfo.getRepoUrl());
        }

        workflowExecutionService.refreshHelmExecutionSummary(context.getWorkflowExecutionId(), summary);
      }
    } catch (Exception ex) {
      logger.info("Exception while updating helm execution summary", ex);
    }
  }
}

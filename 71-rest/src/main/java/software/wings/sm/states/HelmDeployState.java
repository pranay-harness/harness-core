package software.wings.sm.states;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT_SECONDS;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_NAMESPACE_PLACEHOLDER_REGEX;
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
import software.wings.beans.GitFileConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.HelmDummyCommandUnit;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.common.Constants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest.HelmInstallCommandRequestBuilder;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.GitConfigHelperService;
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
import software.wings.utils.KubernetesConvention;

import java.util.ArrayList;
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
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceElement.getUuid());

    ContainerInfrastructureMapping containerInfraMapping =
        (ContainerInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    Activity activity = createActivity(context, getCommandUnits());

    String releaseName = obtainHelmReleaseNamePrefix(context);
    updateHelmReleaseNameInInfraMappingElement(context, releaseName);

    String commandFlags = obtainCommandFlags(context);

    ContainerServiceParams containerServiceParams =
        containerDeploymentHelper.getContainerServiceParams(containerInfraMapping, releaseName, context);

    HelmChartSpecification helmChartSpecification =
        serviceResourceService.getHelmChartSpecification(context.getAppId(), serviceElement.getUuid());

    K8sDelegateManifestConfig sourceRepoConfig = null;
    ApplicationManifest appManifest = applicationManifestService.getAppManifest(
        app.getUuid(), null, serviceElement.getUuid(), AppManifestKind.K8S_MANIFEST);
    if (appManifest != null) {
      GitFileConfig sourceRepoGitFileConfig = appManifest.getGitFileConfig();
      GitConfig sourceRepoGitConfig =
          settingsService.fetchGitConfigFromConnectorId(sourceRepoGitFileConfig.getConnectorId());
      sourceRepoConfig = K8sDelegateManifestConfig.builder()
                             .gitFileConfig(sourceRepoGitFileConfig)
                             .gitConfig(sourceRepoGitConfig)
                             .encryptedDataDetails(fetchEncryptedDataDetail(context, sourceRepoGitConfig))
                             .build();
    }

    if (StateType.HELM_DEPLOY.name().equals(getStateType())) {
      if ((gitFileConfig == null || gitFileConfig.getConnectorId() == null) && sourceRepoConfig == null) {
        validateChartSpecification(helmChartSpecification);
      }
      evaluateHelmChartSpecificationExpression(context, helmChartSpecification);
    }

    HelmDeployStateExecutionData stateExecutionData = HelmDeployStateExecutionData.builder()
                                                          .activityId(activity.getUuid())
                                                          .releaseName(releaseName)
                                                          .namespace(containerServiceParams.getNamespace())
                                                          .commandFlags(commandFlags)
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
        releaseName, app.getAccountId(), app.getUuid(), activity.getUuid(), imageDetails, containerInfraMapping,
        repoName, gitConfig, encryptedDataDetails, commandFlags, sourceRepoConfig);

    delegateService.queueTask(DelegateTask.builder()
                                  .async(true)
                                  .accountId(app.getAccountId())
                                  .appId(app.getUuid())
                                  .waitId(activity.getUuid())
                                  .data(TaskData.builder()
                                            .taskType(TaskType.HELM_COMMAND_TASK.name())
                                            .parameters(new Object[] {commandRequest})
                                            .timeout(TimeUnit.HOURS.toMillis(1))
                                            .build())
                                  .envId(env.getUuid())
                                  .infrastructureMappingId(containerInfraMapping.getUuid())
                                  .build());
    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(singletonList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  protected List<CommandUnit> getCommandUnits() {
    List<CommandUnit> commandUnits = new ArrayList<>();

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
      List<EncryptedDataDetail> encryptedDataDetails, String commandFlags, K8sDelegateManifestConfig sourceRepoConfig) {
    List<String> helmValueOverridesYamlFiles =
        serviceTemplateService.helmValueOverridesYamlFiles(appId, infrastructureMapping.getServiceTemplateId());
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
            .sourceRepoConfig(sourceRepoConfig);

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
                      .taskType(TaskType.HELM_COMMAND_TASK.name())
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

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
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
}

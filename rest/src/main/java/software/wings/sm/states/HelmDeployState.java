package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_NAMESPACE_PLACEHOLDER_REGEX;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.api.HelmDeployStateExecutionData;
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
import software.wings.beans.DelegateTask;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 3/25/18.
 */
public class HelmDeployState extends State {
  @Inject private transient AppService appService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient ContainerDeploymentManagerHelper containerDeploymentHelper;

  @Attributes(title = "Deployment steady state timeout (in minutes).")
  @DefaultValue("10")
  private int steadyStateTimeout; // Minutes

  public static final String HELM_COMMAND_NAME = "Helm Deploy";
  private static final String DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_TAG}";
  private static final String DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_NAME}";

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public HelmDeployState(String name) {
    super(name, StateType.HELM_DEPLOY.name());
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
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE") // TODO
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

    Activity activity = createActivity(context);

    String releaseName = KubernetesConvention.getHelmReleaseName(
        app.getName(), serviceElement.getName(), env.getName(), containerInfraMapping.getUuid());

    ContainerServiceParams containerServiceParams =
        containerDeploymentHelper.getContainerServiceParams(containerInfraMapping, releaseName);

    HelmChartSpecification helmChartSpecification =
        serviceResourceService.getHelmChartSpecification(context.getAppId(), serviceElement.getUuid());
    validateChartSpecification(helmChartSpecification);
    evaluateHelmChartSpecificationExpression(context, helmChartSpecification);

    HelmDeployStateExecutionData stateExecutionData = HelmDeployStateExecutionData.builder()
                                                          .activityId(activity.getUuid())
                                                          .chartName(helmChartSpecification.getChartName())
                                                          .chartRepositoryUrl(helmChartSpecification.getChartUrl())
                                                          .chartVersion(helmChartSpecification.getChartVersion())
                                                          .releaseName(releaseName)
                                                          .build();

    ImageDetails imageDetails = getImageDetails(context, app, artifact);
    String repoName = getRepoName(app.getName(), serviceElement.getName());
    setNewAndPrevReleaseVersion(context, app, releaseName, containerServiceParams, stateExecutionData);
    HelmCommandRequest commandRequest =
        getHelmCommandRequest(context, helmChartSpecification, containerServiceParams, releaseName, app.getAccountId(),
            app.getUuid(), activity.getUuid(), imageDetails, containerInfraMapping, repoName);

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.HELM_COMMAND_TASK)
                                    .withWaitId(activity.getUuid())
                                    .withParameters(new Object[] {commandRequest})
                                    .withEnvId(env.getUuid())
                                    .withTimeout(TimeUnit.HOURS.toMillis(1))
                                    .withInfrastructureMappingId(containerInfraMapping.getUuid())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  protected ImageDetails getImageDetails(ExecutionContext context, Application app, Artifact artifact) {
    return containerDeploymentHelper.fetchArtifactDetails(artifact, app.getUuid(), context.getWorkflowExecutionId());
  }

  protected void setNewAndPrevReleaseVersion(ExecutionContext context, Application app, String releaseName,
      ContainerServiceParams containerServiceParams, HelmDeployStateExecutionData stateExecutionData)
      throws InterruptedException {
    int prevVersion = getPreviousReleaseVersion(app.getUuid(), app.getAccountId(), releaseName, containerServiceParams);

    stateExecutionData.setReleaseOldVersion(prevVersion);
    stateExecutionData.setReleaseNewVersion(prevVersion + 1);
  }

  private void validateChartSpecification(HelmChartSpecification chartSpec) {
    if (chartSpec == null || (isEmpty(chartSpec.getChartName()) && isEmpty(chartSpec.getChartUrl()))) {
      throw new InvalidRequestException(
          "Invalid chart specification " + (chartSpec == null ? "NULL" : chartSpec.toString()));
    }
  }

  @SuppressFBWarnings("DB_DUPLICATE_BRANCHES") // TODO
  protected HelmCommandRequest getHelmCommandRequest(ExecutionContext context,
      HelmChartSpecification helmChartSpecification, ContainerServiceParams containerServiceParams, String releaseName,
      String accountId, String appId, String activityId, ImageDetails imageDetails,
      ContainerInfrastructureMapping infrastructureMapping, String repoName) {
    List<String> helmValueOverridesYamlFiles =
        serviceTemplateService.helmValueOverridesYamlFiles(appId, infrastructureMapping.getServiceTemplateId());
    List<String> helmValueOverridesYamlFilesEvaluated = null;
    if (isNotEmpty(helmValueOverridesYamlFiles)) {
      helmValueOverridesYamlFilesEvaluated =
          helmValueOverridesYamlFiles.stream()
              .map(yamlFileContent
                  -> yamlFileContent.replaceAll(DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX, imageDetails.getTag())
                         .replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX,
                             getImageName(yamlFileContent, imageDetails.getName(), imageDetails.getDomainName()))
                         .replaceAll(HELM_NAMESPACE_PLACEHOLDER_REGEX, infrastructureMapping.getNamespace()))
              .map(context::renderExpression)
              .collect(Collectors.toList());
    }

    steadyStateTimeout = steadyStateTimeout > 0 ? 10 : DEFAULT_STEADY_STATE_TIMEOUT;
    return HelmInstallCommandRequest.builder()
        .appId(appId)
        .accountId(accountId)
        .activityId(activityId)
        .commandName(HELM_COMMAND_NAME)
        .chartSpecification(helmChartSpecification)
        .releaseName(releaseName)
        .namespace(infrastructureMapping.getNamespace())
        .containerServiceParams(containerServiceParams)
        .variableOverridesYamlFiles(helmValueOverridesYamlFilesEvaluated)
        .timeoutInMillis(TimeUnit.MINUTES.toMillis(steadyStateTimeout))
        .repoName(repoName)
        .build();
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

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH") // TODO
  protected int getPreviousReleaseVersion(String appId, String accountId, String releaseName,
      ContainerServiceParams containerServiceParams) throws InterruptedException {
    int prevVersion = 0;
    HelmReleaseHistoryCommandRequest helmReleaseHistoryCommandRequest =
        HelmReleaseHistoryCommandRequest.builder()
            .releaseName(releaseName)
            .containerServiceParams(containerServiceParams)
            .build();
    HelmCommandExecutionResponse helmCommandExecutionResponse =
        delegateService.executeTask(aDelegateTask()
                                        .withTaskType(TaskType.HELM_COMMAND_TASK)
                                        .withParameters(new Object[] {helmReleaseHistoryCommandRequest})
                                        .withAccountId(accountId)
                                        .withAppId(appId)
                                        .withAsync(false)
                                        .build());
    if (helmCommandExecutionResponse != null
        && helmCommandExecutionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
      List<ReleaseInfo> releaseInfoList =
          ((HelmReleaseHistoryCommandResponse) helmCommandExecutionResponse.getHelmCommandResponse())
              .getReleaseInfoList();
      prevVersion = isEmpty(releaseInfoList)
          ? 0
          : Integer.parseInt(releaseInfoList.get(releaseInfoList.size() - 1).getRevision());
    } else {
      throw new InvalidRequestException(helmCommandExecutionResponse.getErrorMessage());
    }
    return prevVersion;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    HelmCommandExecutionResponse executionResponse = (HelmCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    HelmDeployStateExecutionData stateExecutionData = (HelmDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    HelmInstallCommandResponse helmInstallCommandResponse =
        (HelmInstallCommandResponse) executionResponse.getHelmCommandResponse();

    List<InstanceStatusSummary> instanceStatusSummaries = containerDeploymentHelper.getInstanceStatusSummaries(
        context, helmInstallCommandResponse.getContainerInfoList());
    stateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);

    List<InstanceElement> instanceElements =
        instanceStatusSummaries.stream().map(InstanceStatusSummary::getInstanceElement).collect(toList());
    InstanceElementListParam instanceElementListParam =
        InstanceElementListParamBuilder.anInstanceElementListParam().withInstanceElements(instanceElements).build();

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);

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
                                          .commandUnits(Collections.emptyList())
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING)
                                          .commandUnitType(CommandUnitType.HELM);

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
}

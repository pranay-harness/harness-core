package software.wings.sm.states.k8s;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.FeatureName.PRUNE_KUBERNETES_RESOURCES;

import static software.wings.sm.StateType.K8S_BLUE_GREEN_DEPLOY;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.ff.FeatureFlagService;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.k8s.model.K8sPod;
import io.harness.logging.CommandExecutionStatus;
import io.harness.tasks.ResponseData;

import software.wings.api.InstanceElementListParam;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sBlueGreenDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.utils.StateTimeoutUtils;
import software.wings.stencils.DefaultValue;
import software.wings.utils.ApplicationManifestUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(_870_CG_ORCHESTRATION)
@OwnedBy(CDP)
@BreakDependencyOn("software.wings.service.intfc.DelegateService")
public class K8sBlueGreenDeploy extends AbstractK8sState {
  @Inject private transient ActivityService activityService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient AwsCommandHelper awsCommandHelper;
  @Inject private ApplicationManifestUtils applicationManifestUtils;
  @Inject private transient FeatureFlagService featureFlagService;

  public static final String K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME = "Blue/Green Deployment";

  @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") @Getter @Setter private Integer stateTimeoutInMinutes;
  @Getter @Setter @Attributes(title = "Skip Dry Run") private boolean skipDryRun;

  @Override
  public Integer getTimeoutMillis() {
    return StateTimeoutUtils.getTimeoutMillisFromMinutes(stateTimeoutInMinutes);
  }

  public K8sBlueGreenDeploy(String name) {
    super(name, K8S_BLUE_GREEN_DEPLOY.name());
  }

  @Override
  public String commandName() {
    return K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME;
  }

  @Override
  public String stateType() {
    return getStateType();
  }

  @Override
  public void validateParameters(ExecutionContext context) {}

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return executeWrapperWithManifest(this, context, K8sStateHelper.fetchSafeTimeoutInMillis(getTimeoutMillis()));
  }

  @Override
  public ExecutionResponse executeK8sTask(ExecutionContext context, String activityId) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = fetchApplicationManifests(context);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.fetchContainerInfrastructureMapping(context);
    storePreviousHelmDeploymentInfo(context, appManifestMap.get(K8sValuesLocation.Service));

    K8sTaskParameters k8sTaskParameters =
        K8sBlueGreenDeployTaskParameters.builder()
            .activityId(activityId)
            .releaseName(fetchReleaseName(context, infraMapping))
            .commandName(K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.BLUE_GREEN_DEPLOY)
            .timeoutIntervalInMin(stateTimeoutInMinutes)
            .k8sDelegateManifestConfig(
                createDelegateManifestConfig(context, appManifestMap.get(K8sValuesLocation.Service)))
            .valuesYamlList(fetchRenderedValuesFiles(appManifestMap, context))
            .skipDryRun(skipDryRun)
            .skipVersioningForAllK8sObjects(
                appManifestMap.get(K8sValuesLocation.Service).getSkipVersioningForAllK8sObjects())
            .isPruningEnabled(featureFlagService.isEnabled(PRUNE_KUBERNETES_RESOURCES, context.getAccountId()))
            .build();

    return queueK8sDelegateTask(context, k8sTaskParameters, appManifestMap);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    activityService.updateStatus(fetchActivityId(context), fetchAppId(context), executionStatus);

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    if (ExecutionStatus.FAILED == executionStatus) {
      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .stateExecutionData(context.getStateExecutionData())
          .build();
    }

    K8sBlueGreenDeployResponse k8sBlueGreenDeployResponse =
        (K8sBlueGreenDeployResponse) executionResponse.getK8sTaskResponse();

    stateExecutionData.setReleaseNumber(k8sBlueGreenDeployResponse.getReleaseNumber());
    stateExecutionData.setHelmChartInfo(k8sBlueGreenDeployResponse.getHelmChartInfo());
    stateExecutionData.setBlueGreenStageColor(k8sBlueGreenDeployResponse.getStageColor());

    final List<K8sPod> newPods = fetchNewPods(k8sBlueGreenDeployResponse.getK8sPodList());
    InstanceElementListParam instanceElementListParam = fetchInstanceElementListParam(newPods);

    stateExecutionData.setNewInstanceStatusSummaries(
        fetchInstanceStatusSummaries(instanceElementListParam.getInstanceElements(), executionStatus));

    saveK8sElement(context,
        K8sElement.builder()
            .releaseName(stateExecutionData.getReleaseName())
            .releaseNumber(k8sBlueGreenDeployResponse.getReleaseNumber())
            .primaryServiceName(k8sBlueGreenDeployResponse.getPrimaryServiceName())
            .stageServiceName(k8sBlueGreenDeployResponse.getStageServiceName())
            .build());

    saveInstanceInfoToSweepingOutput(context,
        fetchInstanceElementList(k8sBlueGreenDeployResponse.getK8sPodList(), false),
        fetchInstanceDetails(k8sBlueGreenDeployResponse.getK8sPodList(), false));

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .stateExecutionData(stateExecutionData)
        .contextElement(instanceElementListParam)
        .notifyElement(instanceElementListParam)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType, String accountId) {
    List<CommandUnit> blueGreenCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.FetchFiles));
    }

    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Prepare));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Apply));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WaitForSteadyState));
    blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WrapUp));
    if (featureFlagService.isEnabled(PRUNE_KUBERNETES_RESOURCES, accountId)) {
      blueGreenCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Prune));
    }

    return blueGreenCommandUnits;
  }
}

package software.wings.sm.states.k8s;

import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.ff.FeatureFlagService;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.k8s.model.K8sPod;
import io.harness.logging.CommandExecutionStatus;
import io.harness.tasks.ResponseData;

import software.wings.api.InstanceElementListParam;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
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
public class K8sRollingDeploy extends AbstractK8sState {
  @Inject private transient ActivityService activityService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient AwsCommandHelper awsCommandHelper;
  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private ApplicationManifestUtils applicationManifestUtils;

  public static final String K8S_ROLLING_DEPLOY_COMMAND_NAME = "Rolling Deployment";

  @Getter @Setter @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") private Integer stateTimeoutInMinutes;
  @Getter @Setter @Attributes(title = "Skip Dry Run") private boolean skipDryRun;

  @Override
  public Integer getTimeoutMillis() {
    return StateTimeoutUtils.getTimeoutMillisFromMinutes(stateTimeoutInMinutes);
  }

  public K8sRollingDeploy(String name) {
    super(name, K8S_DEPLOYMENT_ROLLING.name());
  }

  @Override
  public String commandName() {
    return K8S_ROLLING_DEPLOY_COMMAND_NAME;
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

    boolean inCanaryFlow = false;
    K8sElement k8sElement = k8sStateHelper.fetchK8sElement(context);
    if (k8sElement != null) {
      inCanaryFlow = k8sElement.isCanary();
    }

    K8sTaskParameters k8sTaskParameters =
        K8sRollingDeployTaskParameters.builder()
            .activityId(activityId)
            .releaseName(fetchReleaseName(context, infraMapping))
            .isInCanaryWorkflow(inCanaryFlow)
            .commandName(K8S_ROLLING_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.DEPLOYMENT_ROLLING)
            .timeoutIntervalInMin(stateTimeoutInMinutes)
            .k8sDelegateManifestConfig(
                createDelegateManifestConfig(context, appManifestMap.get(K8sValuesLocation.Service)))
            .valuesYamlList(fetchRenderedValuesFiles(appManifestMap, context))
            .skipDryRun(skipDryRun)
            .localOverrideFeatureFlag(
                featureFlagService.isEnabled(FeatureName.LOCAL_DELEGATE_CONFIG_OVERRIDE, infraMapping.getAccountId()))
            .skipVersioningForAllK8sObjects(
                appManifestMap.get(K8sValuesLocation.Service).getSkipVersioningForAllK8sObjects())
            .build();

    return queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    Application app = appService.get(context.getAppId());
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    activityService.updateStatus(fetchActivityId(context), app.getUuid(), executionStatus);

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    if (ExecutionStatus.FAILED == executionStatus) {
      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .stateExecutionData(context.getStateExecutionData())
          .build();
    }

    K8sRollingDeployResponse k8sRollingDeployResponse =
        (K8sRollingDeployResponse) executionResponse.getK8sTaskResponse();
    final List<K8sPod> newPods = fetchNewPods(k8sRollingDeployResponse.getK8sPodList());

    stateExecutionData.setReleaseNumber(k8sRollingDeployResponse.getReleaseNumber());
    stateExecutionData.setLoadBalancer(k8sRollingDeployResponse.getLoadBalancer());
    stateExecutionData.setNamespaces(fetchNamespacesFromK8sPodList(newPods));
    stateExecutionData.setHelmChartInfo(k8sRollingDeployResponse.getHelmChartInfo());

    InstanceElementListParam instanceElementListParam = fetchInstanceElementListParam(newPods);

    stateExecutionData.setNewInstanceStatusSummaries(
        fetchInstanceStatusSummaries(instanceElementListParam.getInstanceElements(), executionStatus));

    K8sElement k8sElement = k8sStateHelper.fetchK8sElement(context);
    if (k8sElement == null) {
      // We only want to save if its not there. In case of Canary - we already have it in context.
      saveK8sElement(context,
          K8sElement.builder()
              .releaseName(stateExecutionData.getReleaseName())
              .releaseNumber(k8sRollingDeployResponse.getReleaseNumber())
              .build());
    }

    saveInstanceInfoToSweepingOutput(context, fetchInstanceElementList(k8sRollingDeployResponse.getK8sPodList(), true),
        fetchInstanceDetails(k8sRollingDeployResponse.getK8sPodList(), true));

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
  public List<CommandUnit> commandUnitList(boolean remoteStoreType) {
    List<CommandUnit> rollingDeployCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.FetchFiles));
    }

    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Prepare));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Apply));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WaitForSteadyState));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WrapUp));

    return rollingDeployCommandUnits;
  }
}

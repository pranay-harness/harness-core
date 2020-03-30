package software.wings.sm.states.k8s;

import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.k8s.model.K8sPod;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.InstanceElementListParam;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.FeatureName;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;
import software.wings.utils.ApplicationManifestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sRollingDeploy extends State implements K8sStateExecutor {
  @Inject private transient ActivityService activityService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private transient K8sStateHelper k8sStateHelper;
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient AwsCommandHelper awsCommandHelper;
  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private ApplicationManifestUtils applicationManifestUtils;

  public static final String K8S_ROLLING_DEPLOY_COMMAND_NAME = "Rolling Deployment";

  @Getter @Setter @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") private Integer stateTimeoutInMinutes;
  @Getter @Setter @Attributes(title = "Skip Dry Run") private boolean skipDryRun;

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
    return k8sStateHelper.executeWrapperWithManifest(this, context);
  }

  @Override
  public ExecutionResponse executeK8sTask(ExecutionContext context, String activityId) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

    boolean inCanaryFlow = false;
    K8sElement k8sElement = k8sStateHelper.getK8sElement(context);
    if (k8sElement != null) {
      inCanaryFlow = k8sElement.isCanary();
    }

    K8sTaskParameters k8sTaskParameters =
        K8sRollingDeployTaskParameters.builder()
            .activityId(activityId)
            .releaseName(k8sStateHelper.getReleaseName(context, infraMapping))
            .isInCanaryWorkflow(inCanaryFlow)
            .commandName(K8S_ROLLING_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.DEPLOYMENT_ROLLING)
            .timeoutIntervalInMin(stateTimeoutInMinutes)
            .k8sDelegateManifestConfig(
                k8sStateHelper.createDelegateManifestConfig(context, appManifestMap.get(K8sValuesLocation.Service)))
            .valuesYamlList(k8sStateHelper.getRenderedValuesFiles(appManifestMap, context))
            .skipDryRun(skipDryRun)
            .localOverrideFeatureFlag(
                featureFlagService.isEnabled(FeatureName.LOCAL_DELEGATE_CONFIG_OVERRIDE, infraMapping.getAccountId()))
            .build();

    return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return k8sStateHelper.handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    Application app = appService.get(context.getAppId());
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    activityService.updateStatus(k8sStateHelper.getActivityId(context), app.getUuid(), executionStatus);

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
    final List<K8sPod> newPods = k8sStateHelper.getNewPods(k8sRollingDeployResponse.getK8sPodList());

    stateExecutionData.setReleaseNumber(k8sRollingDeployResponse.getReleaseNumber());
    stateExecutionData.setLoadBalancer(k8sRollingDeployResponse.getLoadBalancer());
    stateExecutionData.setNamespaces(k8sStateHelper.getNamespacesFromK8sPodList(newPods));

    InstanceElementListParam instanceElementListParam = k8sStateHelper.getInstanceElementListParam(newPods);

    stateExecutionData.setNewInstanceStatusSummaries(
        k8sStateHelper.getInstanceStatusSummaries(instanceElementListParam.getInstanceElements(), executionStatus));

    K8sElement k8sElement = k8sStateHelper.getK8sElement(context);
    if (k8sElement == null) {
      // We only want to save if its not there. In case of Canary - we already have it in context.
      k8sStateHelper.saveK8sElement(context,
          K8sElement.builder()
              .releaseName(stateExecutionData.getReleaseName())
              .releaseNumber(k8sRollingDeployResponse.getReleaseNumber())
              .build());
    }

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
      rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.FetchFiles));
    }

    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Prepare));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Apply));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WaitForSteadyState));
    rollingDeployCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WrapUp));

    return rollingDeployCommandUnits;
  }
}

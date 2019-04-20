package software.wings.sm.states.k8s;

import static java.lang.Integer.parseInt;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_CANARY_DEPLOY;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.InstanceElementListParam;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sCanaryDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sCanaryDeployResponse;
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
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sCanaryDeploy extends State implements K8sStateExecutor {
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

  public static final String K8S_CANARY_DEPLOY_COMMAND_NAME = "Canary Deploy";

  public K8sCanaryDeploy(String name) {
    super(name, K8S_CANARY_DEPLOY.name());
  }

  @Getter @Setter @Attributes(title = "Instances") private String instances;
  @Getter @Setter @Attributes(title = "Instance Unit Type") private InstanceUnitType instanceUnitType;

  @Override
  public String commandName() {
    return K8S_CANARY_DEPLOY_COMMAND_NAME;
  }

  @Override
  public String stateType() {
    return getStateType();
  }

  @Override
  public void validateParameters(ExecutionContext context) {
    parseInt(context.renderExpression(this.instances));
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return k8sStateHelper.executeWrapperWithManifest(this, context);
  }

  public ExecutionResponse executeK8sTask(ExecutionContext context, String activityId) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = k8sStateHelper.getApplicationManifests(context);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

    K8sTaskParameters k8sTaskParameters =
        K8sCanaryDeployTaskParameters.builder()
            .activityId(activityId)
            .releaseName(k8sStateHelper.getReleaseName(context, infraMapping))
            .commandName(K8S_CANARY_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.CANARY_DEPLOY)
            .instances(Integer.valueOf(context.renderExpression(this.instances)))
            .instanceUnitType(this.instanceUnitType)
            .timeoutIntervalInMin(10)
            .k8sDelegateManifestConfig(
                k8sStateHelper.createDelegateManifestConfig(context, appManifestMap.get(K8sValuesLocation.Service)))
            .valuesYamlList(k8sStateHelper.getRenderedValuesFiles(appManifestMap, context))
            .build();

    return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return k8sStateHelper.handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    K8sCanaryDeployResponse k8sCanaryDeployResponse = (K8sCanaryDeployResponse) executionResponse.getK8sTaskResponse();

    Integer targetInstances = parseInt(context.renderExpression(this.instances));
    if (k8sCanaryDeployResponse.getCurrentInstances() != null) {
      targetInstances = k8sCanaryDeployResponse.getCurrentInstances();
    }

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();

    stateExecutionData.setReleaseNumber(k8sCanaryDeployResponse.getReleaseNumber());
    stateExecutionData.setTargetInstances(targetInstances);
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    String activityId = stateExecutionData.getActivityId();
    activityService.updateStatus(activityId, appId, executionStatus);

    InstanceElementListParam instanceElementListParam =
        k8sStateHelper.getInstanceElementListParam(k8sCanaryDeployResponse.getK8sPodList());

    stateExecutionData.setNewInstanceStatusSummaries(
        k8sStateHelper.getInstanceStatusSummaries(instanceElementListParam.getInstanceElements(), executionStatus));

    k8sStateHelper.saveK8sElement(context,
        K8sElement.builder()
            .releaseName(stateExecutionData.getReleaseName())
            .releaseNumber(k8sCanaryDeployResponse.getReleaseNumber())
            .targetInstances(targetInstances)
            .isCanary(true)
            .canaryWorkload(k8sCanaryDeployResponse.getCanaryWorkload())
            .build());

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(context.getStateExecutionData())
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType) {
    List<CommandUnit> canaryCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.FetchFiles));
    }

    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Prepare));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Apply));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WaitForSteadyState));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WrapUp));

    return canaryCommandUnits;
  }
}

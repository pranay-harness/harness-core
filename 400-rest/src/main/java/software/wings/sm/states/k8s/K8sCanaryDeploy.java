package software.wings.sm.states.k8s;

import static software.wings.sm.StateType.K8S_CANARY_DEPLOY;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.k8s.model.K8sPod;
import io.harness.logging.CommandExecutionStatus;
import io.harness.tasks.ResponseData;

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
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sCanaryDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.openshift.OpenShiftManagerService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.utils.StateTimeoutUtils;
import software.wings.stencils.DefaultValue;
import software.wings.utils.ApplicationManifestUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sCanaryDeploy extends AbstractK8sState {
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
  @Inject private transient OpenShiftManagerService openShiftManagerService;

  public static final String K8S_CANARY_DEPLOY_COMMAND_NAME = "Canary Deploy";

  public K8sCanaryDeploy(String name) {
    super(name, K8S_CANARY_DEPLOY.name());
  }

  @Getter @Setter @Attributes(title = "Instances") private String instances;
  @Getter @Setter @Attributes(title = "Instance Unit Type") private InstanceUnitType instanceUnitType;
  @Getter @Setter @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") private Integer stateTimeoutInMinutes;
  @Getter @Setter @Attributes(title = "Skip Dry Run") private boolean skipDryRun;

  @Override
  public Integer getTimeoutMillis() {
    return StateTimeoutUtils.getTimeoutMillisFromMinutes(stateTimeoutInMinutes);
  }

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
    validateK8sV2TypeServiceUsed(context);
  }

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
        K8sCanaryDeployTaskParameters.builder()
            .activityId(activityId)
            .releaseName(fetchReleaseName(context, infraMapping))
            .commandName(K8S_CANARY_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.CANARY_DEPLOY)
            .instances(Integer.valueOf(context.renderExpression(this.instances)))
            .instanceUnitType(this.instanceUnitType)
            .timeoutIntervalInMin(stateTimeoutInMinutes)
            .k8sDelegateManifestConfig(
                createDelegateManifestConfig(context, appManifestMap.get(K8sValuesLocation.Service)))
            .valuesYamlList(fetchRenderedValuesFiles(appManifestMap, context))
            .skipDryRun(skipDryRun)
            .skipVersioningForAllK8sObjects(
                appManifestMap.get(K8sValuesLocation.Service).getSkipVersioningForAllK8sObjects())
            .delegateSelectors(this.getDelegateSelectors() == null ? Collections.emptySet()
                                                                   : new HashSet<>(this.getDelegateSelectors()))
            .build();

    return queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    String activityId = stateExecutionData.getActivityId();
    activityService.updateStatus(activityId, appId, executionStatus);

    if (ExecutionStatus.FAILED == executionStatus) {
      if (executionResponse.getK8sTaskResponse() instanceof K8sCanaryDeployResponse) {
        K8sCanaryDeployResponse k8sCanaryDeployResponse =
            (K8sCanaryDeployResponse) executionResponse.getK8sTaskResponse();

        if (isNotBlank(k8sCanaryDeployResponse.getCanaryWorkload())) {
          saveK8sElement(
              context, K8sElement.builder().canaryWorkload(k8sCanaryDeployResponse.getCanaryWorkload()).build());
        }
      }

      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .stateExecutionData(context.getStateExecutionData())
          .build();
    }

    K8sCanaryDeployResponse k8sCanaryDeployResponse = (K8sCanaryDeployResponse) executionResponse.getK8sTaskResponse();

    Integer targetInstances = parseInt(context.renderExpression(this.instances));
    if (k8sCanaryDeployResponse.getCurrentInstances() != null) {
      targetInstances = k8sCanaryDeployResponse.getCurrentInstances();
    }

    stateExecutionData.setReleaseNumber(k8sCanaryDeployResponse.getReleaseNumber());
    stateExecutionData.setTargetInstances(targetInstances);
    stateExecutionData.setHelmChartInfo(k8sCanaryDeployResponse.getHelmChartInfo());

    final List<K8sPod> newPods = fetchNewPods(k8sCanaryDeployResponse.getK8sPodList());
    InstanceElementListParam instanceElementListParam = fetchInstanceElementListParam(newPods);

    stateExecutionData.setNewInstanceStatusSummaries(
        fetchInstanceStatusSummaries(instanceElementListParam.getInstanceElements(), executionStatus));

    saveK8sElement(context,
        K8sElement.builder()
            .releaseName(stateExecutionData.getReleaseName())
            .releaseNumber(k8sCanaryDeployResponse.getReleaseNumber())
            .targetInstances(targetInstances)
            .isCanary(true)
            .canaryWorkload(k8sCanaryDeployResponse.getCanaryWorkload())
            .build());

    saveInstanceInfoToSweepingOutput(context, fetchInstanceElementList(k8sCanaryDeployResponse.getK8sPodList(), false),
        fetchInstanceDetails(k8sCanaryDeployResponse.getK8sPodList(), false));

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .stateExecutionData(context.getStateExecutionData())
        .contextElement(instanceElementListParam)
        .notifyElement(instanceElementListParam)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType) {
    List<CommandUnit> canaryCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      canaryCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.FetchFiles));
    }

    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Prepare));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Apply));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WaitForSteadyState));
    canaryCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.WrapUp));

    return canaryCommandUnits;
  }
}

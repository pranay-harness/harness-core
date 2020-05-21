package software.wings.sm.states.k8s;

import static software.wings.sm.StateType.K8S_SCALE;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.K8sPod;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.InstanceElementListParam;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sScaleTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.response.K8sScaleResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sScale extends State {
  @Inject private transient ConfigService configService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
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

  public static final String K8S_SCALE_COMMAND_NAME = "Scale";

  public K8sScale(String name) {
    super(name, K8S_SCALE.name());
  }

  @Getter @Setter @Attributes(title = "Workload") private String workload;
  @Getter @Setter @Attributes(title = "Instances") private String instances;
  @Getter @Setter @Attributes(title = "Instance Unit Type") private InstanceUnitType instanceUnitType;
  @Getter @Setter @Attributes(title = "Skip steady state check") private boolean skipSteadyStateCheck;
  @Getter @Setter @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") private Integer stateTimeoutInMinutes;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

      Integer maxInstances = null;
      K8sElement k8sElement = k8sStateHelper.getK8sElement(context);
      if (k8sElement != null) {
        maxInstances = k8sElement.getTargetInstances();
      }

      Activity activity = createActivity(context);

      K8sTaskParameters k8sTaskParameters = K8sScaleTaskParameters.builder()
                                                .activityId(activity.getUuid())
                                                .releaseName(k8sStateHelper.getReleaseName(context, infraMapping))
                                                .commandName(K8S_SCALE_COMMAND_NAME)
                                                .k8sTaskType(K8sTaskType.SCALE)
                                                .workload(context.renderExpression(this.workload))
                                                .instances(Integer.valueOf(context.renderExpression(this.instances)))
                                                .instanceUnitType(this.instanceUnitType)
                                                .maxInstances(maxInstances)
                                                .skipSteadyStateCheck(this.skipSteadyStateCheck)
                                                .timeoutIntervalInMin(stateTimeoutInMinutes)
                                                .build();

      return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();
      K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

      ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
          ? ExecutionStatus.SUCCESS
          : ExecutionStatus.FAILED;

      K8sScaleResponse k8sScaleResponse = (K8sScaleResponse) executionResponse.getK8sTaskResponse();

      activityService.updateStatus(k8sStateHelper.getActivityId(context), appId, executionStatus);

      K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);
      stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

      if (ExecutionStatus.FAILED == executionStatus) {
        return ExecutionResponse.builder()
            .executionStatus(executionStatus)
            .stateExecutionData(context.getStateExecutionData())
            .build();
      }

      final List<K8sPod> newPods = k8sStateHelper.getNewPods(k8sScaleResponse.getK8sPodList());
      InstanceElementListParam instanceElementListParam = k8sStateHelper.getInstanceElementListParam(newPods);

      stateExecutionData.setNewInstanceStatusSummaries(
          k8sStateHelper.getInstanceStatusSummaries(instanceElementListParam.getInstanceElements(), executionStatus));

      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .stateExecutionData(context.getStateExecutionData())
          .contextElement(instanceElementListParam)
          .notifyElement(instanceElementListParam)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private Activity createActivity(ExecutionContext context) {
    if (this.skipSteadyStateCheck) {
      return k8sStateHelper.createK8sActivity(context, K8S_SCALE_COMMAND_NAME, getStateType(), activityService,
          ImmutableList.of(
              new K8sDummyCommandUnit(K8sDummyCommandUnit.Init), new K8sDummyCommandUnit(K8sDummyCommandUnit.Scale)));
    } else {
      return k8sStateHelper.createK8sActivity(context, K8S_SCALE_COMMAND_NAME, getStateType(), activityService,
          ImmutableList.of(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init),
              new K8sDummyCommandUnit(K8sDummyCommandUnit.Scale),
              new K8sDummyCommandUnit(K8sDummyCommandUnit.WaitForSteadyState)));
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}

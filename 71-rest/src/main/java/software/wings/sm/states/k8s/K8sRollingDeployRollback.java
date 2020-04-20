package software.wings.sm.states.k8s;

import static io.harness.beans.ExecutionStatus.SKIPPED;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING_ROLLBACK;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
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
import lombok.Getter;
import lombok.Setter;
import software.wings.api.k8s.K8sContextElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployRollbackTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
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

import java.time.Duration;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sRollingDeployRollback extends State {
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

  public static final String K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME = "Rolling Deployment Rollback";

  @Getter @Setter @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") private Integer stateTimeoutInMinutes;

  public K8sRollingDeployRollback(String name) {
    super(name, K8S_DEPLOYMENT_ROLLING_ROLLBACK.name());
  }

  @Override
  public Integer getTimeoutMillis() {
    if (stateTimeoutInMinutes != null) {
      return Ints.checkedCast(Duration.ofMinutes(stateTimeoutInMinutes).toMillis());
    }
    return null;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      K8sContextElement k8sContextElement = context.getContextElement(ContextElementType.K8S);

      if (k8sContextElement == null) {
        return ExecutionResponse.builder()
            .executionStatus(SKIPPED)
            .stateExecutionData(aStateExecutionData().withErrorMsg("No context found for rollback. Skipping.").build())
            .build();
      }

      Activity activity = k8sStateHelper.createK8sActivity(context, K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME,
          getStateType(), activityService,
          ImmutableList.of(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init),
              new K8sDummyCommandUnit(K8sDummyCommandUnit.Rollback),
              new K8sDummyCommandUnit(K8sDummyCommandUnit.WaitForSteadyState)));

      K8sTaskParameters k8sTaskParameters = K8sRollingDeployRollbackTaskParameters.builder()
                                                .activityId(activity.getUuid())
                                                .releaseName(k8sContextElement.getReleaseName())
                                                .releaseNumber(k8sContextElement.getReleaseNumber())
                                                .commandName(K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME)
                                                .k8sTaskType(K8sTaskType.DEPLOYMENT_ROLLING_ROLLBACK)
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

      activityService.updateStatus(k8sStateHelper.getActivityId(context), appId, executionStatus);

      K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);

      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .stateExecutionData(context.getStateExecutionData())
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}

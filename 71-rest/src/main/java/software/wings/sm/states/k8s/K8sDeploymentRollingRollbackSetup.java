package software.wings.sm.states.k8s;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.k8s.K8sDeploymentRollingSetupStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.common.Constants;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest.K8sCommandType;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingRollbackSetupRequest;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;

import java.util.Arrays;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sDeploymentRollingRollbackSetup extends State {
  private static final transient Logger logger = LoggerFactory.getLogger(K8sDeploymentRollingRollbackSetup.class);

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

  public static final String K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME = "Rolling Deployment Rollback";

  public K8sDeploymentRollingRollbackSetup(String name) {
    super(name, K8S_DEPLOYMENT_ROLLING.name());
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Application app = appService.get(context.getAppId());
      Environment env = workflowStandardParams.getEnv();

      ContainerInfrastructureMapping infraMapping = (ContainerInfrastructureMapping) infrastructureMappingService.get(
          app.getUuid(), phaseElement.getInfraMappingId());

      Activity activity = k8sStateHelper.createK8sActivity(context, K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME,
          getStateType(), activityService,
          ImmutableList.of(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init),
              new K8sDummyCommandUnit(K8sDummyCommandUnit.Rollback),
              new K8sDummyCommandUnit(K8sDummyCommandUnit.StatusCheck)));

      K8sCommandRequest commandRequest =
          K8sDeploymentRollingRollbackSetupRequest.builder()
              .activityId(activity.getUuid())
              .appId(app.getUuid())
              .accountId(app.getAccountId())
              .infraMappingId(infraMapping.getUuid())
              .commandName(K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME)
              .k8sCommandType(K8sCommandType.DEPLOYMENT_ROLLING_ROLLBACK)
              .k8sClusterConfig(containerDeploymentManagerHelper.getK8sClusterConfig(infraMapping))
              .workflowExecutionId(context.getWorkflowExecutionId())
              .timeoutIntervalInMin(10)
              .build();

      DelegateTask delegateTask =
          aDelegateTask()
              .withAccountId(app.getAccountId())
              .withAppId(app.getUuid())
              .withTaskType(TaskType.K8S_COMMAND_TASK)
              .withWaitId(activity.getUuid())
              .withParameters(new Object[] {commandRequest})
              .withEnvId(env.getUuid())
              .withTimeout(getTimeoutMillis() != null ? getTimeoutMillis() : DEFAULT_ASYNC_CALL_TIMEOUT)
              .withInfrastructureMappingId(infraMapping.getUuid())
              .build();

      String delegateTaskId = delegateService.queueTask(delegateTask);

      return ExecutionResponse.Builder.anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Arrays.asList(activity.getUuid()))
          .withStateExecutionData(K8sDeploymentRollingSetupStateExecutionData.builder()
                                      .activityId(activity.getUuid())
                                      .commandName(K8S_DEPLOYMENT_ROLLING_ROLLBACK_COMMAND_NAME)
                                      .build())
          .withDelegateTaskId(delegateTaskId)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();
      String activityId = response.keySet().iterator().next();
      K8sCommandExecutionResponse executionResponse = (K8sCommandExecutionResponse) response.values().iterator().next();

      ExecutionStatus executionStatus =
          executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                               : ExecutionStatus.FAILED;

      activityService.updateStatus(activityId, appId, executionStatus);

      K8sDeploymentRollingSetupStateExecutionData stateExecutionData =
          (K8sDeploymentRollingSetupStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);

      return anExecutionResponse()
          .withExecutionStatus(executionStatus)
          .withStateExecutionData(context.getStateExecutionData())
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}

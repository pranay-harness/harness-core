package software.wings.sm.states.k8s;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_CANARY_ROLLBACK;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.k8s.K8sContextElement;
import software.wings.api.k8s.K8sSetupExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sCanaryRollbackTaskParameters;
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
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;

import java.util.Arrays;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sCanaryRollback extends State {
  private static final transient Logger logger = LoggerFactory.getLogger(K8sCanaryRollback.class);

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

  public static final String K8S_CANARY_ROLLBACK_COMMAND_NAME = "Canary Rollback";

  public K8sCanaryRollback(String name) {
    super(name, K8S_CANARY_ROLLBACK.name());
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
      Application app = appService.get(context.getAppId());

      ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

      K8sContextElement k8sContextElement = context.getContextElement(ContextElementType.K8S);

      Activity activity =
          k8sStateHelper.createK8sActivity(context, K8S_CANARY_ROLLBACK_COMMAND_NAME, getStateType(), activityService,
              ImmutableList.of(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init),
                  new K8sDummyCommandUnit(K8sDummyCommandUnit.Rollback)));

      K8sTaskParameters taskParameters =
          K8sCanaryRollbackTaskParameters.builder()
              .activityId(activity.getUuid())
              .appId(app.getUuid())
              .accountId(app.getAccountId())
              .releaseName(k8sContextElement.getReleaseName())
              .releaseNumber(k8sContextElement.getReleaseNumber())
              .targetReplicas(k8sContextElement.getTargetInstances())
              .commandName(K8S_CANARY_ROLLBACK_COMMAND_NAME)
              .k8sTaskType(K8sTaskType.CANARY_ROLLBACK)
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
              .withTags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(taskParameters))
              .withParameters(new Object[] {taskParameters})
              .withEnvId(k8sStateHelper.getEnvironment(context).getUuid())
              .withTimeout(getTimeoutMillis() != null ? getTimeoutMillis() : DEFAULT_ASYNC_CALL_TIMEOUT)
              .withInfrastructureMappingId(infraMapping.getUuid())
              .build();

      String delegateTaskId = delegateService.queueTask(delegateTask);

      return ExecutionResponse.Builder.anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Arrays.asList(activity.getUuid()))
          .withStateExecutionData(K8sSetupExecutionData.builder()
                                      .activityId(activity.getUuid())
                                      .commandName(K8S_CANARY_ROLLBACK_COMMAND_NAME)
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
      K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

      ExecutionStatus executionStatus =
          executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                               : ExecutionStatus.FAILED;

      activityService.updateStatus(activityId, appId, executionStatus);

      K8sSetupExecutionData stateExecutionData = (K8sSetupExecutionData) context.getStateExecutionData();
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

package software.wings.sm.states;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ECS_STEADY_STATE_CHECK;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.PhaseElement;
import software.wings.api.ScriptStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.EcsSteadyStateCheckParams;
import software.wings.beans.container.EcsSteadyStateCheckResponse;
import software.wings.common.Constants;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Validator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EcsSteadyStateCheck extends State {
  public static final String ECS_STEADY_STATE_CHECK_COMMAND_NAME = "Ecs Steady State Check";

  @Inject private transient AppService appService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient ActivityService activityService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient ContainerDeploymentManagerHelper containerDeploymentManagerHelper;

  @Attributes(title = "Ecs Service") @Getter @Setter private String ecsServiceName;

  public EcsSteadyStateCheck(String name) {
    super(name, ECS_STEADY_STATE_CHECK.name());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Application app = appService.get(context.getAppId());
      Environment env = workflowStandardParams.getEnv();
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
      if (!(infrastructureMapping instanceof EcsInfrastructureMapping)) {
        throw new InvalidRequestException(
            String.format("Infrmapping with id: %s is not Ecs inframapping", phaseElement.getInfraMappingId()));
      }
      EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
      Activity activity = createActivity(context);
      AwsConfig awsConfig = getAwsConfig(ecsInfrastructureMapping.getComputeProviderSettingId());
      EcsSteadyStateCheckParams params =
          EcsSteadyStateCheckParams.builder()
              .appId(app.getUuid())
              .region(ecsInfrastructureMapping.getRegion())
              .accountId(app.getAccountId())
              .timeoutInMs(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
              .activityId(activity.getUuid())
              .commandName(ECS_STEADY_STATE_CHECK_COMMAND_NAME)
              .clusterName(ecsInfrastructureMapping.getClusterName())
              .serviceName(context.renderExpression(ecsServiceName))
              .awsConfig(awsConfig)
              .encryptionDetails(secretManager.getEncryptionDetails(awsConfig, GLOBAL_APP_ID, null))
              .build();
      DelegateTask delegateTask =
          DelegateTask.builder()
              .async(true)
              .accountId(app.getAccountId())
              .appId(app.getUuid())
              .taskType(TaskType.ECS_STEADY_STATE_CHECK_TASK.name())
              .waitId(activity.getUuid())
              .tags(isNotEmpty(params.getAwsConfig().getTag()) ? singletonList(params.getAwsConfig().getTag()) : null)
              .data(TaskData.builder()
                        .parameters(new Object[] {params})
                        .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                        .build())
              .envId(env.getUuid())
              .build();
      String delegateTaskId = delegateService.queueTask(delegateTask);
      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(singletonList(activity.getUuid()))
          .withStateExecutionData(ScriptStateExecutionData.builder().activityId(activity.getUuid()).build())
          .withDelegateTaskId(delegateTaskId)
          .build();
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();
      String activityId = response.keySet().iterator().next();
      EcsSteadyStateCheckResponse ecsSteadyStateCheckResponse =
          (EcsSteadyStateCheckResponse) response.values().iterator().next();
      activityService.updateStatus(activityId, appId, ecsSteadyStateCheckResponse.getExecutionStatus());
      ScriptStateExecutionData stateExecutionData = (ScriptStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(ecsSteadyStateCheckResponse.getExecutionStatus());
      stateExecutionData.setDelegateMetaInfo(ecsSteadyStateCheckResponse.getDelegateMetaInfo());

      List<InstanceStatusSummary> instanceStatusSummaries = containerDeploymentManagerHelper.getInstanceStatusSummaries(
          context, ecsSteadyStateCheckResponse.getContainerInfoList());
      List<InstanceElement> instanceElements =
          instanceStatusSummaries.stream().map(InstanceStatusSummary::getInstanceElement).collect(toList());
      InstanceElementListParam instanceElementListParam =
          InstanceElementListParamBuilder.anInstanceElementListParam().withInstanceElements(instanceElements).build();

      return anExecutionResponse()
          .withExecutionStatus(ecsSteadyStateCheckResponse.getExecutionStatus())
          .withStateExecutionData(context.getStateExecutionData())
          .addContextElement(instanceElementListParam)
          .addNotifyElement(instanceElementListParam)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  private AwsConfig getAwsConfig(String awsConfigId) {
    SettingAttribute awsSettingAttribute = settingsService.get(awsConfigId);
    Validator.notNullCheck("awsSettingAttribute", awsSettingAttribute);
    if (!(awsSettingAttribute.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("Invalid Aws Config Id");
    }
    return (AwsConfig) awsSettingAttribute.getValue();
  }

  private Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(ECS_STEADY_STATE_CHECK_COMMAND_NAME)
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .status(ExecutionStatus.RUNNING)
                                          .commandUnitType(CommandUnitType.ECS_STEADY_STATE_CHECK)
                                          .triggeredBy(TriggeredBy.builder()
                                                           .email(workflowStandardParams.getCurrentUser().getEmail())
                                                           .name(workflowStandardParams.getCurrentUser().getName())
                                                           .build());

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
}

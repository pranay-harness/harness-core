package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.KubernetesSteadyStateCheckExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.KubernetesSteadyStateCheckParams;
import software.wings.beans.container.Label;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KubernetesSteadyStateCheck extends State {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesSteadyStateCheck.class);

  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ActivityService activityService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;

  @Getter @Setter @Attributes(title = "Labels") private List<Label> labels = Lists.newArrayList();

  public static final String KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME = "Kubernetes Steady State Check";

  public KubernetesSteadyStateCheck(String name) {
    super(name, StateType.KUBERNETES_STEADY_STATE_CHECK.name());
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
      return executeInternal(context);
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, NotifyResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String activityId = response.keySet().iterator().next();
    KubernetesSteadyStateCheckResponse executionResponse =
        (KubernetesSteadyStateCheckResponse) response.values().iterator().next();
    activityService.updateStatus(activityId, appId, executionResponse.getExecutionStatus());

    KubernetesSteadyStateCheckExecutionData stateExecutionData =
        (KubernetesSteadyStateCheckExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionResponse.getExecutionStatus());

    List<InstanceStatusSummary> instanceStatusSummaries =
        containerDeploymentManagerHelper.getInstanceStatusSummaries(context, executionResponse.getContainerInfoList());
    stateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);

    List<InstanceElement> instanceElements =
        instanceStatusSummaries.stream().map(InstanceStatusSummary::getInstanceElement).collect(toList());
    InstanceElementListParam instanceElementListParam =
        InstanceElementListParamBuilder.anInstanceElementListParam().withInstanceElements(instanceElements).build();

    return anExecutionResponse()
        .withExecutionStatus(executionResponse.getExecutionStatus())
        .withStateExecutionData(context.getStateExecutionData())
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  protected Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME)
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING)
                                          .commandUnitType(CommandUnitType.KUBERNETES_STEADY_STATE_CHECK);
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

  private ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ContainerInfrastructureMapping containerInfraMapping =
        (ContainerInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    if (CollectionUtils.isEmpty(labels)) {
      throw new InvalidRequestException("Labels cannot be empty.");
    }

    labels.forEach(label -> label.setValue(context.renderExpression(label.getValue())));

    Map<String, String> labelMap =
        labels.stream().collect(Collectors.toMap(label -> label.getName(), label -> label.getValue()));

    Activity activity = createActivity(context);
    KubernetesSteadyStateCheckParams kubernetesSteadyStateCheckParams =
        KubernetesSteadyStateCheckParams.builder()
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .activityId(activity.getUuid())
            .commandName(KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME)
            .containerServiceParams(
                containerDeploymentManagerHelper.getContainerServiceParams(containerInfraMapping, ""))
            .labels(labelMap)
            .timeoutMillis(getTimeoutMillis() != null ? getTimeoutMillis() : DEFAULT_ASYNC_CALL_TIMEOUT)
            .build();
    DelegateTask delegateTask =
        aDelegateTask()
            .withAccountId(app.getAccountId())
            .withAppId(app.getUuid())
            .withTaskType(TaskType.KUBERNETES_STEADY_STATE_CHECK_TASK)
            .withWaitId(activity.getUuid())
            .withParameters(new Object[] {kubernetesSteadyStateCheckParams})
            .withEnvId(env.getUuid())
            .withTimeout(getTimeoutMillis() != null ? getTimeoutMillis() : DEFAULT_ASYNC_CALL_TIMEOUT)
            .withInfrastructureMappingId(containerInfraMapping.getUuid())
            .build();
    String delegateTaskId = delegateService.queueTask(delegateTask);
    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(KubernetesSteadyStateCheckExecutionData.builder()
                                    .activityId(activity.getUuid())
                                    .labels(labels)
                                    .commandName(KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME)
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }
}

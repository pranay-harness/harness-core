package software.wings.sm.states;

import static io.harness.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
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
import software.wings.beans.Environment;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.KubernetesSteadyStateCheckParams;
import software.wings.beans.container.Label;
import software.wings.common.Constants;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.container.ContainerMasterUrlHelper;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KubernetesSteadyStateCheck extends State {
  public static final long DEFAULT_SYNC_CALL_TIMEOUT = 60 * 1000; // 1 minute

  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private transient ContainerMasterUrlHelper containerMasterUrlHelper;

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

      Map<String, String> labelMap = labels.stream().collect(Collectors.toMap(Label::getName, Label::getValue));

      ContainerServiceParams containerServiceParams =
          containerDeploymentManagerHelper.getContainerServiceParams(containerInfraMapping, "", context);

      if (containerMasterUrlHelper.masterUrlRequired(containerInfraMapping)) {
        boolean masterUrlPresent = containerMasterUrlHelper.fetchMasterUrlAndUpdateInfraMapping(
            containerInfraMapping, containerServiceParams, getSyncContext(context, containerInfraMapping));
        if (!masterUrlPresent) {
          throw new InvalidRequestException("No Valid Master Url for" + containerInfraMapping.getClass().getName()
                  + "Id : " + containerInfraMapping.getUuid(),
              USER);
        }
      }

      Activity activity = createActivity(context);
      KubernetesSteadyStateCheckParams kubernetesSteadyStateCheckParams =
          KubernetesSteadyStateCheckParams.builder()
              .accountId(app.getAccountId())
              .appId(app.getUuid())
              .activityId(activity.getUuid())
              .commandName(KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME)
              .containerServiceParams(containerServiceParams)
              .labels(labelMap)
              .timeoutMillis(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
              .build();
      DelegateTask delegateTask = DelegateTask.builder()
                                      .async(true)
                                      .accountId(app.getAccountId())
                                      .appId(app.getUuid())
                                      .waitId(activity.getUuid())
                                      .data(TaskData.builder()
                                                .taskType(TaskType.KUBERNETES_STEADY_STATE_CHECK_TASK.name())
                                                .parameters(new Object[] {kubernetesSteadyStateCheckParams})
                                                .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                                                .build())
                                      .envId(env.getUuid())
                                      .infrastructureMappingId(containerInfraMapping.getUuid())
                                      .build();
      String delegateTaskId = delegateService.queueTask(delegateTask);
      return ExecutionResponse.Builder.anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(singletonList(activity.getUuid()))
          .withStateExecutionData(
              KubernetesSteadyStateCheckExecutionData.builder()
                  .activityId(activity.getUuid())
                  .labels(labels)
                  .commandName(KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME)
                  .namespace(kubernetesSteadyStateCheckParams.getContainerServiceParams().getNamespace())
                  .build())
          .withDelegateTaskId(delegateTaskId)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();
      String activityId = response.keySet().iterator().next();
      KubernetesSteadyStateCheckResponse executionResponse =
          (KubernetesSteadyStateCheckResponse) response.values().iterator().next();
      activityService.updateStatus(activityId, appId, executionResponse.getExecutionStatus());

      KubernetesSteadyStateCheckExecutionData stateExecutionData =
          (KubernetesSteadyStateCheckExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionResponse.getExecutionStatus());

      List<InstanceStatusSummary> instanceStatusSummaries = containerDeploymentManagerHelper.getInstanceStatusSummaries(
          context, executionResponse.getContainerInfoList());
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
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
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

  private SyncTaskContext getSyncContext(
      ExecutionContext context, ContainerInfrastructureMapping containerInfrastructureMapping) {
    return SyncTaskContext.builder()
        .accountId(context.getAccountId())
        .appId(context.getAppId())
        .envId(containerInfrastructureMapping.getEnvId())
        .infrastructureMappingId(containerInfrastructureMapping.getUuid())
        .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 2)
        .build();
  }
}

package software.wings.sm.states;

import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.common.Constants.PRIMARY_SERVICE_NAME_EXPRESSION;
import static software.wings.common.Constants.STAGE_SERVICE_NAME_EXPRESSION;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceElement;
import software.wings.api.InstanceElement;
import software.wings.api.KubernetesSwapServiceSelectorsExecutionData;
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
import software.wings.beans.container.KubernetesSwapServiceSelectorsParams;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
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
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class KubernetesSwapServiceSelectors extends State {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesSwapServiceSelectors.class);

  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient ActivityService activityService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;

  @Getter @Setter @Attributes(title = "Service One") private String service1;

  @Getter @Setter @Attributes(title = "Service Two") private String service2;

  public static final String KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME = "Kubernetes Swap Service Selectors";

  public KubernetesSwapServiceSelectors(String name) {
    super(name, StateType.KUBERNETES_SWAP_SERVICE_SELECTORS.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (InvalidRequestException e) {
      throw e;
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
    KubernetesSwapServiceSelectorsResponse executionResponse =
        (KubernetesSwapServiceSelectorsResponse) response.values().iterator().next();
    activityService.updateStatus(activityId, appId, executionResponse.getExecutionStatus());

    KubernetesSwapServiceSelectorsExecutionData stateExecutionData =
        (KubernetesSwapServiceSelectorsExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionResponse.getExecutionStatus());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    return anExecutionResponse()
        .withExecutionStatus(executionResponse.getExecutionStatus())
        .withStateExecutionData(context.getStateExecutionData())
        .build();
  }

  protected Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);
    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME)
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
                                          .commandUnitType(CommandUnitType.KUBERNETES_SWAP_SERVICE_SELECTORS);
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

  private static String getRenderedServiceName(
      ExecutionContext context, String baseServiceName, String serviceNameExpression) {
    if (StringUtils.equals(PRIMARY_SERVICE_NAME_EXPRESSION, serviceNameExpression)) {
      if (StringUtils.isEmpty(baseServiceName)) {
        throw new InvalidRequestException(
            "Service Name cannot to inferred from context. You have to specify a valid service Name instead of expression: "
            + serviceNameExpression);
      }
      return KubernetesConvention.getPrimaryServiceName(KubernetesConvention.getKubernetesServiceName(baseServiceName));
    }

    if (StringUtils.equals(STAGE_SERVICE_NAME_EXPRESSION, serviceNameExpression)) {
      if (StringUtils.isEmpty(baseServiceName)) {
        throw new InvalidRequestException(
            "Service Name cannot to inferred from context. You have to specify a valid service Name instead of expression: "
            + serviceNameExpression);
      }
      return KubernetesConvention.getStageServiceName(KubernetesConvention.getKubernetesServiceName(baseServiceName));
    }

    return KubernetesConvention.getKubernetesServiceName(context.renderExpression(serviceNameExpression));
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    if (StringUtils.isEmpty(service1) || StringUtils.isEmpty(service2)) {
      throw new InvalidRequestException("Service Name cannot be empty");
    }

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ContainerInfrastructureMapping containerInfraMapping =
        (ContainerInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    ContainerServiceElement containerElement =
        context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
            .stream()
            .filter(cse -> phaseElement.getDeploymentType().equals(cse.getDeploymentType().name()))
            .filter(cse -> phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(ContainerServiceElement.builder().build());

    String baseServiceName = containerElement.getControllerNamePrefix();
    String renderedService1 = getRenderedServiceName(context, baseServiceName, service1);
    String renderedService2 = getRenderedServiceName(context, baseServiceName, service2);

    Activity activity = createActivity(context);
    KubernetesSwapServiceSelectorsParams kubernetesSwapServiceSelectorsParams =
        KubernetesSwapServiceSelectorsParams.builder()
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .activityId(activity.getUuid())
            .commandName(KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME)
            .containerServiceParams(
                containerDeploymentManagerHelper.getContainerServiceParams(containerInfraMapping, ""))
            .service1(renderedService1)
            .service2(renderedService2)
            .build();
    DelegateTask delegateTask =
        aDelegateTask()
            .withAccountId(app.getAccountId())
            .withAppId(app.getUuid())
            .withTaskType(TaskType.KUBERNETES_SWAP_SERVICE_SELECTORS_TASK)
            .withWaitId(activity.getUuid())
            .withParameters(new Object[] {kubernetesSwapServiceSelectorsParams})
            .withEnvId(env.getUuid())
            .withTimeout(getTimeoutMillis() != null ? getTimeoutMillis() : DEFAULT_ASYNC_CALL_TIMEOUT)
            .withInfrastructureMappingId(containerInfraMapping.getUuid())
            .build();
    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(KubernetesSwapServiceSelectorsExecutionData.builder()
                                    .activityId(activity.getUuid())
                                    .service1(renderedService1)
                                    .service2(renderedService2)
                                    .commandName(KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME)
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }
}

package software.wings.sm.states;

import static software.wings.api.PhaseStepSubWorkflowExecutionData.PhaseStepSubWorkflowExecutionDataBuilder.aPhaseStepSubWorkflowExecutionData;
import static software.wings.sm.ServiceInstancesProvisionState.ServiceInstancesProvisionStateBuilder.aServiceInstancesProvisionState;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.DeploymentType;
import software.wings.api.EcsServiceElement;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepSubWorkflowExecutionData;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.ErrorCode;
import software.wings.beans.FailureStrategy;
import software.wings.beans.PhaseStepType;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseStepSubWorkflow extends SubWorkflowState {
  public PhaseStepSubWorkflow(String name) {
    super(name, StateType.PHASE_STEP.name());
  }

  private PhaseStepType phaseStepType;
  private boolean stepsInParallel;
  private boolean defaultFailureStrategy;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  // Only for rollback phases
  @SchemaIgnore private String rollbackPhaseStepName;

  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    if (phaseStepType == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "null phaseStepType");
    }
    ExecutionResponse response = super.execute(contextIntf);

    PhaseElement phaseElement = contextIntf.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    switch (phaseStepType) {
      case CONTAINER_DEPLOY: {
        validateEcsServiceElement(contextIntf, phaseElement);
        break;
      }
      case DEPLOY_SERVICE:
      case ENABLE_SERVICE:
      case DISABLE_SERVICE: {
        validateServiceInstanceIdsParams(contextIntf);
        break;
      }
    }

    response.setStateExecutionData(aPhaseStepSubWorkflowExecutionData()
                                       .withStepsInParallel(stepsInParallel)
                                       .withDefaultFailureStrategy(defaultFailureStrategy)
                                       .withFailureStrategies(failureStrategies)
                                       .build());
    return response;
  }

  private void validateServiceInstanceIdsParams(ExecutionContext contextIntf) {}

  private void validateEcsServiceElement(ExecutionContext context, PhaseElement phaseElement) {
    List<ContextElement> elements = context.getContextElementList(ContextElementType.ECS_SERVICE);
    if (elements == null || elements.isEmpty()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "ECS Service Setup not done");
    }

    Optional<ContextElement> ecsServiceElement =
        elements.parallelStream()
            .filter(contextElement
                -> contextElement instanceof EcsServiceElement && contextElement.getUuid() != null
                    && contextElement.getUuid().equals(phaseElement.getServiceElement().getUuid()))
            .findFirst();

    if (!ecsServiceElement.isPresent()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          "ecsServiceElement not present for the service " + phaseElement.getServiceElement().getUuid());
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    if (phaseStepType == PhaseStepType.PRE_DEPLOYMENT || phaseStepType == PhaseStepType.POST_DEPLOYMENT) {
      return executionResponse;
    }
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    handleElementNotifyResponseData(context, phaseElement, response, executionResponse);

    super.handleStatusSummary(context, response, executionResponse);
    return executionResponse;
  }

  private void handleElementNotifyResponseData(ExecutionContext context, PhaseElement phaseElement,
      Map<String, NotifyResponseData> response, ExecutionResponse executionResponse) {
    if (phaseElement.getDeploymentType().equals(DeploymentType.SSH.name())
        && phaseStepType == PhaseStepType.PROVISION_NODE) {
      ServiceInstanceIdsParam serviceInstanceIdsParam = (ServiceInstanceIdsParam) notifiedElement(
          response, ServiceInstanceIdsParam.class, "Missing ServiceInstanceIdsParam");

      PhaseStepSubWorkflowExecutionData phaseStepSubWorkflowExecutionData =
          (PhaseStepSubWorkflowExecutionData) context.getStateExecutionData();
      phaseStepSubWorkflowExecutionData.setPhaseStepExecutionState(
          aServiceInstancesProvisionState()
              .withInstanceIds(serviceInstanceIdsParam.getInstanceIds())
              .withServiceId(serviceInstanceIdsParam.getServiceId())
              .build());
      executionResponse.setStateExecutionData(phaseStepSubWorkflowExecutionData);
      executionResponse.setContextElements(Lists.newArrayList(serviceInstanceIdsParam));
    } else if (phaseElement.getDeploymentType().equals(DeploymentType.ECS.name())
        && phaseStepType == PhaseStepType.CONTAINER_SETUP) {
      EcsServiceElement ecsServiceElement =
          (EcsServiceElement) notifiedElement(response, EcsServiceElement.class, "Missing ECSServiceElement");
      executionResponse.setContextElements(Lists.newArrayList(ecsServiceElement));
    }
  }

  private ContextElement notifiedElement(
      Map<String, NotifyResponseData> response, Class<? extends ContextElement> cls, String message) {
    if (response == null || response.isEmpty()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
    }
    NotifyResponseData notifiedResponseData = response.values().iterator().next();

    if (!(notifiedResponseData instanceof ElementNotifyResponseData)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
    }
    ElementNotifyResponseData elementNotifyResponseData = (ElementNotifyResponseData) notifiedResponseData;
    List<ContextElement> elements = elementNotifyResponseData.getContextElements();
    if (elements == null || elements.isEmpty()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
    }
    if (!(cls.isInstance(elements.get(0)))) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", message);
    }

    return elements.get(0);
  }

  public boolean isStepsInParallel() {
    return stepsInParallel;
  }

  public void setStepsInParallel(boolean stepsInParallel) {
    this.stepsInParallel = stepsInParallel;
  }

  public boolean isDefaultFailureStrategy() {
    return defaultFailureStrategy;
  }

  public void setDefaultFailureStrategy(boolean defaultFailureStrategy) {
    this.defaultFailureStrategy = defaultFailureStrategy;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  @SchemaIgnore
  public String getRollbackPhaseStepName() {
    return rollbackPhaseStepName;
  }

  public void setRollbackPhaseStepName(String rollbackPhaseStepName) {
    this.rollbackPhaseStepName = rollbackPhaseStepName;
  }

  @SchemaIgnore
  public PhaseStepType getPhaseStepType() {
    return phaseStepType;
  }

  public void setPhaseStepType(PhaseStepType phaseStepType) {
    this.phaseStepType = phaseStepType;
  }
}

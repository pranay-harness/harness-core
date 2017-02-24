package software.wings.sm.states;

import static software.wings.api.PhaseStepSubWorkflowExecutionData.PhaseStepSubWorkflowExecutionDataBuilder.aPhaseStepSubWorkflowExecutionData;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.sm.ServiceInstancesProvisionState.ServiceInstancesProvisionStateBuilder.aServiceInstancesProvisionState;

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
import software.wings.sm.ServiceInstancesProvisionState;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    if (phaseStepType == PhaseStepType.CONTAINER_DEPLOY) {
      List<ContextElement> elements = contextIntf.getContextElementList(ContextElementType.ECS_SERVICE);
      if (elements == null) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "ECS Service Setup not done");
      }

      EcsServiceElement ecsServiceElement = null;
      for (ContextElement element : elements) {
        if (!(elements.get(0) instanceof EcsServiceElement)) {
          continue;
        }
        if (((EcsServiceElement) element).getUuid().equals(phaseElement.getServiceElement().getUuid())) {
          ecsServiceElement = (EcsServiceElement) element;
          break;
        }
      }

      if (ecsServiceElement == null) {
        throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "ecsServiceElement not present");
      }
    }
    if ((phaseStepType == PhaseStepType.DEPLOY_SERVICE || phaseStepType == PhaseStepType.ENABLE_SERVICE
            || phaseStepType == PhaseStepType.DISABLE_SERVICE)
        && response instanceof SpawningExecutionResponse
        && !((SpawningExecutionResponse) response).getStateExecutionInstanceList().isEmpty()) {
      ServiceInstancesProvisionState serviceInstancesProvisionState =
          (ServiceInstancesProvisionState) contextIntf.evaluateExpression(Constants.WINGS_VARIABLE_PREFIX
              + Constants.PROVISION_NODE_NAME + ".phaseStepExecutionState" + Constants.WINGS_VARIABLE_SUFFIX);

      ServiceInstanceIdsParam serviceInstanceIdsParam =
          aServiceInstanceIdsParam()
              .withServiceId(serviceInstancesProvisionState.getServiceId())
              .withInstanceIds(serviceInstancesProvisionState.getInstanceIds())
              .build();

      ((SpawningExecutionResponse) response).getStateExecutionInstanceList().forEach(instance -> {
        instance.getContextElements().push(serviceInstanceIdsParam);
      });
    }
    response.setStateExecutionData(aPhaseStepSubWorkflowExecutionData()
                                       .withStepsInParallel(stepsInParallel)
                                       .withDefaultFailureStrategy(defaultFailureStrategy)
                                       .withFailureStrategies(failureStrategies)
                                       .build());
    return response;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    if (phaseStepType == PhaseStepType.PRE_DEPLOYMENT || phaseStepType == PhaseStepType.POST_DEPLOYMENT) {
      return executionResponse;
    }
    NotifyResponseData notifyResponseData = response.values().iterator().next();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    handleElementNotifyResponseData(context, phaseElement, notifyResponseData, executionResponse);

    super.handleStatusSummary(context, response, executionResponse);
    return executionResponse;
  }

  private void handleElementNotifyResponseData(ExecutionContext context, PhaseElement phaseElement,
      NotifyResponseData notifyResponseData, ExecutionResponse executionResponse) {
    if (phaseElement.getDeploymentType().equals(DeploymentType.SSH.name())
        && phaseStepType == PhaseStepType.PROVISION_NODE) {
      List<ContextElement> elements = notifiedElements(notifyResponseData);
      if (!(elements.get(0) instanceof ServiceInstanceIdsParam)) {
        throw new WingsException(ErrorCode.UNKNOWN_ERROR);
      }

      ServiceInstanceIdsParam serviceInstanceIdsParam = (ServiceInstanceIdsParam) elements.get(0);
      PhaseStepSubWorkflowExecutionData phaseStepSubWorkflowExecutionData =
          (PhaseStepSubWorkflowExecutionData) context.getStateExecutionData();
      phaseStepSubWorkflowExecutionData.setPhaseStepExecutionState(
          aServiceInstancesProvisionState()
              .withInstanceIds(serviceInstanceIdsParam.getInstanceIds())
              .withServiceId(serviceInstanceIdsParam.getServiceId())
              .build());
      executionResponse.setStateExecutionData(phaseStepSubWorkflowExecutionData);
    } else if (phaseElement.getDeploymentType().equals(DeploymentType.ECS.name())
        && phaseStepType == PhaseStepType.CONTAINER_SETUP) {
      List<ContextElement> elements = notifiedElements(notifyResponseData);
      if (!(elements.get(0) instanceof EcsServiceElement)) {
        throw new WingsException(ErrorCode.UNKNOWN_ERROR);
      }

      EcsServiceElement ecsServiceElement = (EcsServiceElement) elements.get(0);
      List<ContextElement> responseElements = executionResponse.getElements();
      if (responseElements == null) {
        responseElements = new ArrayList<>();
        executionResponse.setElements(responseElements);
      }
      responseElements.add(ecsServiceElement);
    }
  }

  private List<ContextElement> notifiedElements(NotifyResponseData notifyResponseData) {
    if (!(notifyResponseData instanceof ElementNotifyResponseData)) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR);
    }
    ElementNotifyResponseData elementNotifyResponseData = (ElementNotifyResponseData) notifyResponseData;
    List<ContextElement> elements = elementNotifyResponseData.getContextElements();
    if (elements == null || elements.isEmpty()) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR);
    }
    return elements;
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

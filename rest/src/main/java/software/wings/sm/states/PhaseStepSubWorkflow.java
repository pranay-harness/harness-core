package software.wings.sm.states;

import static software.wings.api.PhaseStepSubWorkflowExecutionData.PhaseStepSubWorkflowExecutionDataBuilder.aPhaseStepSubWorkflowExecutionData;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.sm.ServiceInstancesProvisionState.ServiceInstancesProvisionStateBuilder.aServiceInstancesProvisionState;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepSubWorkflowExecutionData;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.ErrorCodes;
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

  PhaseStepType phaseStepType;
  private boolean stepsInParallel;
  private boolean defaultFailureStrategy;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  // Only for rollback phases
  @SchemaIgnore private boolean rollback;
  @SchemaIgnore private String rollbackPhaseStepName;

  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    if (phaseStepType == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "null phaseStepType");
    }
    ExecutionResponse response = super.execute(contextIntf);

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

    if (phaseElement.getDeploymentType() == DeploymentType.SSH && phaseStepType == PhaseStepType.PROVISION_NODE) {
      if (!(notifyResponseData instanceof ElementNotifyResponseData)) {
        throw new WingsException(ErrorCodes.UNKNOWN_ERROR);
      }
      ElementNotifyResponseData elementNotifyResponseData = (ElementNotifyResponseData) notifyResponseData;
      List<ContextElement> elements = elementNotifyResponseData.getContextElements();
      if (elements == null || elements.isEmpty() || !(elements.get(0) instanceof ServiceInstanceIdsParam)) {
        throw new WingsException(ErrorCodes.UNKNOWN_ERROR);
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
    }

    return super.handleAsyncResponse(context, response);
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
  public boolean isRollback() {
    return rollback;
  }

  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  @SchemaIgnore
  public String getRollbackPhaseStepName() {
    return rollbackPhaseStepName;
  }

  public void setRollbackPhaseStepName(String rollbackPhaseStepName) {
    this.rollbackPhaseStepName = rollbackPhaseStepName;
  }
}

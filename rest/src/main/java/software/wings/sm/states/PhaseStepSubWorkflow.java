package software.wings.sm.states;

import software.wings.beans.FailureStrategy;
import software.wings.sm.ContextElement;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
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

  private boolean stepsInParallel;
  private boolean defaultFailureStrategy;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = super.handleAsyncResponse(context, response);
    NotifyResponseData notifyResponseData = response.values().iterator().next();
    if (notifyResponseData instanceof ElementNotifyResponseData) {
      ElementNotifyResponseData elementNotifyResponseData = (ElementNotifyResponseData) notifyResponseData;
      List<ContextElement> elements = elementNotifyResponseData.getContextElements();
      if (elements != null && !elements.isEmpty()) {
        executionResponse.setElements(elements);
      }
    }
    return executionResponse;
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
}

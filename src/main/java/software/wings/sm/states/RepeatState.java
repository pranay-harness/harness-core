/**
 *
 */

package software.wings.sm.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 */
public class RepeatState extends State {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private ContextElementType repeatElementType;
  private String repeatElementExpression;
  private RepeatStrategy repeatStrategy;

  private String repeatTransitionStateName;

  public RepeatState(String name) {
    super(name, StateType.REPEAT.name());
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) context.getStateExecutionData();
    return execute((ExecutionContextImpl) context, repeatStateExecutionData);
  }

  ExecutionResponse execute(ExecutionContextImpl context, RepeatStateExecutionData repeatStateExecutionData) {
    if (repeatStateExecutionData == null) {
      repeatStateExecutionData = new RepeatStateExecutionData();
    }
    List<ContextElement> repeatElements = repeatStateExecutionData.getRepeatElements();
    try {
      if (repeatElements == null || repeatElements.size() == 0) {
        if (repeatElementExpression != null) {
          repeatElements = context.evaluateRepeatExpression(repeatElementType, repeatElementExpression);
          repeatStateExecutionData.setRepeatElements(repeatElements);
        }
      }
    } catch (Exception ex) {
      logger.error("Error in getting repeat elements", ex);
    }

    if (repeatElements == null || repeatElements.size() == 0) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage(
          "No repeat elements found for the expression - repeatElementExpression:" + repeatElementExpression);
      return executionResponse;
    }

    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();

    if (repeatStrategy == RepeatStrategy.PARALLEL) {
      for (ContextElement repeatElement : repeatElements) {
        processChildState(stateExecutionInstance, correlationIds, executionResponse, repeatElement);
      }
    } else {
      Integer repeatElementIndex = 0;
      repeatStateExecutionData.setRepeatElementIndex(0);
      ContextElement repeatElement = repeatElements.get(repeatElementIndex);
      processChildState(stateExecutionInstance, correlationIds, executionResponse, repeatElement);
    }

    executionResponse.setAsynch(true);
    executionResponse.setCorrelationIds(correlationIds);
    executionResponse.setStateExecutionData(repeatStateExecutionData);
    return executionResponse;
  }

  private void processChildState(StateExecutionInstance stateExecutionInstance, List<String> correlationIds,
      SpawningExecutionResponse executionResponse, ContextElement repeatElement) {
    String notifyId = stateExecutionInstance.getUuid() + "-repeat-" + repeatElement.getName();
    StateExecutionInstance childStateExecutionInstance =
        JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);

    childStateExecutionInstance.setStateName(repeatTransitionStateName);
    childStateExecutionInstance.setNotifyId(notifyId);

    childStateExecutionInstance.getContextElements().push(repeatElement);
    executionResponse.add(childStateExecutionInstance);
    correlationIds.add(notifyId);
  }

  @Override
  public ExecutionResponse handleAsynchResponse(
      ExecutionContextImpl context, Map<String, ? extends Serializable> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    for (Serializable status : response.values()) {
      executionStatus = (ExecutionStatus) status;
      if (executionStatus != ExecutionStatus.SUCCESS) {
        break;
      }
    }

    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) context.getStateExecutionData();
    List<ContextElement> repeatElements = repeatStateExecutionData.getRepeatElements();

    if (repeatStrategy == RepeatStrategy.PARALLEL || executionStatus == ExecutionStatus.FAILED
        || repeatStateExecutionData.indexReachedMax()) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(executionStatus);
      return executionResponse;
    } else {
      SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();

      Integer repeatElementIndex = repeatStateExecutionData.getRepeatElementIndex();
      repeatElementIndex++;
      repeatStateExecutionData.setRepeatElementIndex(repeatElementIndex);
      ContextElement repeatElement = repeatElements.get(repeatElementIndex);

      StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
      List<String> correlationIds = new ArrayList<>();
      processChildState(stateExecutionInstance, correlationIds, executionResponse, repeatElement);

      executionResponse.setAsynch(true);
      executionResponse.setCorrelationIds(correlationIds);
      executionResponse.setStateExecutionData(repeatStateExecutionData);
      return executionResponse;
    }
  }

  public ContextElementType getRepeatElementType() {
    return repeatElementType;
  }

  public void setRepeatElementType(ContextElementType repeatElementType) {
    this.repeatElementType = repeatElementType;
  }

  public String getRepeatElementExpression() {
    return repeatElementExpression;
  }

  public void setRepeatElementExpression(String repeatElementExpression) {
    this.repeatElementExpression = repeatElementExpression;
  }

  public RepeatStrategy getRepeatStrategy() {
    return repeatStrategy;
  }

  public void setRepeatStrategy(RepeatStrategy repeatStrategy) {
    this.repeatStrategy = repeatStrategy;
  }

  public String getRepeatTransitionStateName() {
    return repeatTransitionStateName;
  }

  public void setRepeatTransitionStateName(String repeatTransitionStateName) {
    this.repeatTransitionStateName = repeatTransitionStateName;
  }

  public enum RepeatStrategy { SERIAL, PARALLEL }

  public static class RepeatStateExecutionData extends StateExecutionData {
    private static final long serialVersionUID = 5043797016447183954L;
    private List<ContextElement> repeatElements = new ArrayList<>();
    private Integer repeatElementIndex;

    public List<ContextElement> getRepeatElements() {
      return repeatElements;
    }

    public void setRepeatElements(List<ContextElement> repeatElements) {
      this.repeatElements = repeatElements;
    }

    public Integer getRepeatElementIndex() {
      return repeatElementIndex;
    }

    public void setRepeatElementIndex(Integer repeatElementIndex) {
      this.repeatElementIndex = repeatElementIndex;
    }

    boolean indexReachedMax() {
      if (repeatElements != null && repeatElementIndex != null && repeatElementIndex == repeatElements.size()) {
        return true;
      } else {
        return false;
      }
    }
  }
}

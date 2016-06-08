/**
 *
 */

package software.wings.sm.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ExecutionStrategy;
import software.wings.exception.WingsException;
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

// TODO: Auto-generated Javadoc

/**
 * The Class RepeatState.
 *
 * @author Rishi
 */
public class RepeatState extends State {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private ContextElementType repeatElementType;
  private String repeatElementExpression;
  private ExecutionStrategy executionStrategy;
  private String executionStrategyExpression;

  private String repeatTransitionStateName;

  /**
   * Instantiates a new repeat state.
   *
   * @param name the name
   */
  public RepeatState(String name) {
    super(name, StateType.REPEAT.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) context.getStateExecutionData();
    return execute((ExecutionContextImpl) context, repeatStateExecutionData);
  }

  /**
   * Execute.
   *
   * @param context                  the context
   * @param repeatStateExecutionData the repeat state execution data
   * @return the execution response
   */
  ExecutionResponse execute(ExecutionContextImpl context, RepeatStateExecutionData repeatStateExecutionData) {
    if (repeatStateExecutionData == null) {
      repeatStateExecutionData = new RepeatStateExecutionData();
    }
    List<ContextElement> repeatElements = repeatStateExecutionData.getRepeatElements();
    try {
      if (repeatElements == null || repeatElements.size() == 0) {
        if (repeatElementExpression != null) {
          repeatElements = (List<ContextElement>) context.evaluateExpression(repeatElementExpression);
          repeatStateExecutionData.setRepeatElements(repeatElements);
        }
      }
    } catch (Exception ex) {
      logger.error("Error in getting repeat elements", ex);
      throw new WingsException(ex);
    }

    if (repeatElements == null || repeatElements.size() == 0) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage(
          "No repeat elements found for the expression - repeatElementExpression:" + repeatElementExpression);
      return executionResponse;
    }
    if (repeatTransitionStateName == null) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage("No repeatTransitionStateName defined");
      return executionResponse;
    }

    if (executionStrategyExpression != null) {
      try {
        executionStrategy = (ExecutionStrategy) context.evaluateExpression(executionStrategyExpression);
      } catch (Exception ex) {
        logger.error("Error in evaluating executionStrategy... default to SERIAL", ex);
        executionStrategy = ExecutionStrategy.SERIAL;
      }
    }

    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();

    if (executionStrategy == ExecutionStrategy.PARALLEL) {
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

  /* (non-Javadoc)
   * @see software.wings.sm.State#handleAsynchResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
   */
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

    if (executionStrategy == ExecutionStrategy.PARALLEL || executionStatus == ExecutionStatus.FAILED
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

  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  public String getExecutionStrategyExpression() {
    return executionStrategyExpression;
  }

  public void setExecutionStrategyExpression(String executionStrategyExpression) {
    this.executionStrategyExpression = executionStrategyExpression;
  }

  public String getRepeatTransitionStateName() {
    return repeatTransitionStateName;
  }

  public void setRepeatTransitionStateName(String repeatTransitionStateName) {
    this.repeatTransitionStateName = repeatTransitionStateName;
  }

  /**
   * The Class RepeatStateExecutionData.
   */
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

    /**
     * Index reached max.
     *
     * @return true, if successful
     */
    boolean indexReachedMax() {
      if (repeatElements != null && repeatElementIndex != null && repeatElementIndex == repeatElements.size()) {
        return true;
      } else {
        return false;
      }
    }
  }
}

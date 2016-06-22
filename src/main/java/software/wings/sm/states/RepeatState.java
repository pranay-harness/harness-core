/**
 *
 */

package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ExecutionStrategy;
import software.wings.common.WingsExpressionProcessorFactory;
import software.wings.exception.WingsException;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExpressionProcessor;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Auto-generated Javadoc

/**
 * The Class RepeatState.
 *
 * @author Rishi
 */
@Attributes(title = "Repeat")
public class RepeatState extends State {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @SchemaIgnore private ContextElementType repeatElementType;
  @Attributes(required = true, title = "Repeat Element Expression") private String repeatElementExpression;
  @Attributes(required = true, title = "Execution Strategy") private ExecutionStrategy executionStrategy;
  @SchemaIgnore private String executionStrategyExpression;

  @SchemaIgnore private String repeatTransitionStateName;

  /**
   * Instantiates a new repeat state.
   *
   * @param name the name
   */
  public RepeatState(String name) {
    super(name, StateType.REPEAT.name());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) context.getStateExecutionData();
    return execute((ExecutionContextImpl) context, repeatStateExecutionData);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResponse handleAsyncResponse(
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

      executionResponse.setAsync(true);
      executionResponse.setCorrelationIds(correlationIds);
      executionResponse.setStateExecutionData(repeatStateExecutionData);
      return executionResponse;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resolveProperties() {
    ExpressionProcessor expressionProcessor =
        WingsExpressionProcessorFactory.getMatchingExpressionProcessor(repeatElementExpression, null);
    repeatElementType =
        expressionProcessor == null ? ContextElementType.OTHER : expressionProcessor.getContextElementType();
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
          "No repeat elements found for the repeatElementExpression:" + repeatElementExpression);
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
      }
    }

    if (executionStrategy == null) {
      logger.info("RepeatState: {} falling to default  executionStrategy : SERIAL", getName());
      executionStrategy = ExecutionStrategy.SERIAL;
    }

    repeatStateExecutionData.setExecutionStrategy(executionStrategy);

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

    executionResponse.setAsync(true);
    executionResponse.setCorrelationIds(correlationIds);
    executionResponse.setStateExecutionData(repeatStateExecutionData);
    return executionResponse;
  }

  private void processChildState(StateExecutionInstance stateExecutionInstance, List<String> correlationIds,
      SpawningExecutionResponse executionResponse, ContextElement repeatElement) {
    processChildState(stateExecutionInstance, correlationIds, executionResponse, repeatElement, null);
  }

  private void processChildState(StateExecutionInstance stateExecutionInstance, List<String> correlationIds,
      SpawningExecutionResponse executionResponse, ContextElement repeatElement, String prevInstanceId) {
    String notifyId = stateExecutionInstance.getUuid() + "-repeat-" + repeatElement.getUuid();
    StateExecutionInstance childStateExecutionInstance =
        JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
    childStateExecutionInstance.setStateName(repeatTransitionStateName);
    childStateExecutionInstance.setNotifyId(notifyId);
    childStateExecutionInstance.setPrevInstanceId(prevInstanceId);

    childStateExecutionInstance.getContextElements().push(repeatElement);
    childStateExecutionInstance.setContextElementName(repeatElement.getName());
    childStateExecutionInstance.setContextElementType(repeatElement.getElementType().name());
    executionResponse.add(childStateExecutionInstance);
    correlationIds.add(notifyId);
  }

  /**
   * Gets repeat element type.
   *
   * @return the repeat element type
   */
  @SchemaIgnore
  public ContextElementType getRepeatElementType() {
    return repeatElementType;
  }

  /**
   * Sets repeat element type.
   *
   * @param repeatElementType the repeat element type
   */
  @SchemaIgnore
  public void setRepeatElementType(ContextElementType repeatElementType) {
    this.repeatElementType = repeatElementType;
  }

  /**
   * Gets repeat element expression.
   *
   * @return the repeat element expression
   */
  public String getRepeatElementExpression() {
    return repeatElementExpression;
  }

  /**
   * Sets repeat element expression.
   *
   * @param repeatElementExpression the repeat element expression
   */
  public void setRepeatElementExpression(String repeatElementExpression) {
    this.repeatElementExpression = repeatElementExpression;
  }

  /**
   * Gets execution strategy.
   *
   * @return the execution strategy
   */
  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  /**
   * Sets execution strategy.
   *
   * @param executionStrategy the execution strategy
   */
  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  /**
   * Gets execution strategy expression.
   *
   * @return the execution strategy expression
   */
  public String getExecutionStrategyExpression() {
    return executionStrategyExpression;
  }

  /**
   * Sets execution strategy expression.
   *
   * @param executionStrategyExpression the execution strategy expression
   */
  public void setExecutionStrategyExpression(String executionStrategyExpression) {
    this.executionStrategyExpression = executionStrategyExpression;
  }

  /**
   * Gets repeat transition state name.
   *
   * @return the repeat transition state name
   */
  @SchemaIgnore
  public String getRepeatTransitionStateName() {
    return repeatTransitionStateName;
  }

  /**
   * Sets repeat transition state name.
   *
   * @param repeatTransitionStateName the repeat transition state name
   */
  @SchemaIgnore
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
    private ExecutionStrategy executionStrategy;

    /**
     * Gets repeat elements.
     *
     * @return the repeat elements
     */
    public List<ContextElement> getRepeatElements() {
      return repeatElements;
    }

    /**
     * Sets repeat elements.
     *
     * @param repeatElements the repeat elements
     */
    public void setRepeatElements(List<ContextElement> repeatElements) {
      this.repeatElements = repeatElements;
    }

    /**
     * Gets repeat element index.
     *
     * @return the repeat element index
     */
    public Integer getRepeatElementIndex() {
      return repeatElementIndex;
    }

    /**
     * Sets repeat element index.
     *
     * @param repeatElementIndex the repeat element index
     */
    public void setRepeatElementIndex(Integer repeatElementIndex) {
      this.repeatElementIndex = repeatElementIndex;
    }

    /**
     * Index reached max.
     *
     * @return true, if successful
     */
    boolean indexReachedMax() {
      if (repeatElements != null && repeatElementIndex != null && repeatElementIndex == (repeatElements.size() - 1)) {
        return true;
      } else {
        return false;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getExecutionSummary() {
      LinkedHashMap<String, Object> execData = fillExecutionData();
      execData.putAll((Map<String, Object>) super.getExecutionSummary());
      return execData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getExecutionDetails() {
      LinkedHashMap<String, Object> execData = fillExecutionData();
      execData.putAll((Map<String, Object>) super.getExecutionSummary());
      return execData;
    }

    private LinkedHashMap<String, Object> fillExecutionData() {
      LinkedHashMap<String, Object> orderedMap = new LinkedHashMap<>();
      putNotNull(orderedMap, "repeatElements",
          repeatElements.stream().map(ContextElement::getName).collect(Collectors.toList()).toString());
      putNotNull(orderedMap, "executionStrategy", executionStrategy);
      return orderedMap;
    }

    /**
     * Getter for property 'executionStrategy'.
     *
     * @return Value for property 'executionStrategy'.
     */
    public ExecutionStrategy getExecutionStrategy() {
      return executionStrategy;
    }

    /**
     * Setter for property 'executionStrategy'.
     *
     * @param executionStrategy Value to set for property 'executionStrategy'.
     */
    public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
      this.executionStrategy = executionStrategy;
    }
  }
}

/**
 *
 */

package software.wings.sm.states;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.Joiner;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.ExecutionStrategy;
import software.wings.common.Constants;
import software.wings.common.WingsExpressionProcessorFactory;
import software.wings.exception.WingsException;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExpressionProcessor;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Class RepeatState.
 *
 * @author Rishi
 */
@Attributes
public class RepeatState extends State {
  private static final Logger logger = LoggerFactory.getLogger(RepeatState.class);

  @SchemaIgnore private ContextElementType repeatElementType;
  @DefaultValue("")
  @Attributes(required = true, title = "Repeat Element Expression")
  private String repeatElementExpression;
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
          repeatStateExecutionData.setRepeatElementType(repeatElements.get(0).getElementType());
        }
      }
    } catch (Exception ex) {
      logger.error("Error in getting repeat elements", ex);
      throw new WingsException(ex);
    }

    if (repeatElements == null || repeatElements.size() == 0) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage("Expression: " + repeatElementExpression + ". no repeat elements found");
      return executionResponse;
    }
    if (repeatTransitionStateName == null) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    for (Object status : response.values()) {
      ExecutionStatus responseStatus = ((ElementNotifyResponseData) status).getExecutionStatus();
      if (responseStatus != ExecutionStatus.SUCCESS) {
        executionStatus = responseStatus;
      }
    }

    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) context.getStateExecutionData();
    List<ContextElement> repeatElements = repeatStateExecutionData.getRepeatElements();

    executionStrategy = repeatStateExecutionData.getExecutionStrategy();
    if (executionStrategy == ExecutionStrategy.PARALLEL || executionStatus == ExecutionStatus.FAILED
        || repeatStateExecutionData.indexReachedMax()) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(executionStatus);

      executionResponse.setStateExecutionData(repeatStateExecutionData);
      return executionResponse;
    } else {
      SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();

      Integer repeatElementIndex = repeatStateExecutionData.getRepeatElementIndex();
      repeatElementIndex++;
      repeatStateExecutionData.setRepeatElementIndex(repeatElementIndex);
      ContextElement repeatElement = repeatElements.get(repeatElementIndex);

      StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();
      List<String> correlationIds = new ArrayList<>();
      processChildState(stateExecutionInstance, correlationIds, executionResponse, repeatElement);

      executionResponse.setAsync(true);
      executionResponse.setCorrelationIds(correlationIds);

      executionResponse.setStateExecutionData(repeatStateExecutionData);
      return executionResponse;
    }
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

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

  private void processChildState(StateExecutionInstance stateExecutionInstance, List<String> correlationIds,
      SpawningExecutionResponse executionResponse, ContextElement repeatElement) {
    String notifyId = stateExecutionInstance.getUuid() + "-repeat-" + repeatElement.getUuid();
    StateExecutionInstance childStateExecutionInstance =
        JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
    childStateExecutionInstance.setStateName(repeatTransitionStateName);
    childStateExecutionInstance.setNotifyId(notifyId);
    childStateExecutionInstance.setPrevInstanceId(null);

    childStateExecutionInstance.getContextElements().push(repeatElement);
    childStateExecutionInstance.setContextElement(repeatElement);
    childStateExecutionInstance.setContextTransition(true);
    childStateExecutionInstance.setStatus(ExecutionStatus.NEW);
    childStateExecutionInstance.setStartTs(null);
    childStateExecutionInstance.setEndTs(null);
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

  @Override
  public int hashCode() {
    return Objects.hash(logger, repeatElementType, repeatElementExpression, executionStrategy,
        executionStrategyExpression, repeatTransitionStateName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final RepeatState other = (RepeatState) obj;
    return Objects.equals(this.logger, other.logger) && Objects.equals(this.repeatElementType, other.repeatElementType)
        && Objects.equals(this.repeatElementExpression, other.repeatElementExpression)
        && Objects.equals(this.executionStrategy, other.executionStrategy)
        && Objects.equals(this.executionStrategyExpression, other.executionStrategyExpression)
        && Objects.equals(this.repeatTransitionStateName, other.repeatTransitionStateName);
  }

  /**
   * The Class RepeatStateExecutionData.
   */
  public static class RepeatStateExecutionData extends ElementStateExecutionData {
    private ContextElementType repeatElementType;
    private List<ContextElement> repeatElements = new ArrayList<>();
    private Integer repeatElementIndex;
    private ExecutionStrategy executionStrategy;

    public ContextElementType getRepeatElementType() {
      return repeatElementType;
    }

    public void setRepeatElementType(ContextElementType repeatElementType) {
      this.repeatElementType = repeatElementType;
    }

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
    public Map<String, ExecutionDataValue> getExecutionDetails() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
      putNotNull(executionDetails, "repeatElements",
          anExecutionDataValue()
              .withValue(Joiner.on(", ").join(
                  repeatElements.stream().map(ContextElement::getName).collect(Collectors.toList())))
              .withDisplayName("Repeating Over")
              .build());
      putNotNull(executionDetails, "executionStrategy",
          anExecutionDataValue().withValue(executionStrategy).withDisplayName("Execution Strategy").build());
      return executionDetails;
    }

    @Override
    public Map<String, ExecutionDataValue> getExecutionSummary() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
      putNotNull(executionDetails, "repeatElements",
          anExecutionDataValue()
              .withValue(
                  abbreviate(Joiner.on(", ").join(
                                 repeatElements.stream().map(ContextElement::getName).collect(Collectors.toList())),
                      Constants.SUMMARY_PAYLOAD_LIMIT))
              .withDisplayName("Repeating Over")
              .build());
      putNotNull(executionDetails, "executionStrategy",
          anExecutionDataValue().withValue(executionStrategy).withDisplayName("Execution Strategy").build());
      return executionDetails;
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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private ContextElementType repeatElementType;
    private String repeatElementExpression;
    private ExecutionStrategy executionStrategy;
    private String executionStrategyExpression;
    private String repeatTransitionStateName;

    private Builder() {}

    /**
     * A repeat state builder.
     *
     * @return the builder
     */
    public static Builder aRepeatState() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With repeat element type builder.
     *
     * @param repeatElementType the repeat element type
     * @return the builder
     */
    public Builder withRepeatElementType(ContextElementType repeatElementType) {
      this.repeatElementType = repeatElementType;
      return this;
    }

    /**
     * With repeat element expression builder.
     *
     * @param repeatElementExpression the repeat element expression
     * @return the builder
     */
    public Builder withRepeatElementExpression(String repeatElementExpression) {
      this.repeatElementExpression = repeatElementExpression;
      return this;
    }

    /**
     * With execution strategy builder.
     *
     * @param executionStrategy the execution strategy
     * @return the builder
     */
    public Builder withExecutionStrategy(ExecutionStrategy executionStrategy) {
      this.executionStrategy = executionStrategy;
      return this;
    }

    /**
     * With execution strategy expression builder.
     *
     * @param executionStrategyExpression the execution strategy expression
     * @return the builder
     */
    public Builder withExecutionStrategyExpression(String executionStrategyExpression) {
      this.executionStrategyExpression = executionStrategyExpression;
      return this;
    }

    /**
     * With repeat transition state name builder.
     *
     * @param repeatTransitionStateName the repeat transition state name
     * @return the builder
     */
    public Builder withRepeatTransitionStateName(String repeatTransitionStateName) {
      this.repeatTransitionStateName = repeatTransitionStateName;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aRepeatState()
          .withName(name)
          .withRepeatElementType(repeatElementType)
          .withRepeatElementExpression(repeatElementExpression)
          .withExecutionStrategy(executionStrategy)
          .withExecutionStrategyExpression(executionStrategyExpression)
          .withRepeatTransitionStateName(repeatTransitionStateName);
    }

    /**
     * Build repeat state.
     *
     * @return the repeat state
     */
    public RepeatState build() {
      RepeatState repeatState = new RepeatState(name);
      repeatState.setRepeatElementType(repeatElementType);
      repeatState.setRepeatElementExpression(repeatElementExpression);
      repeatState.setExecutionStrategy(executionStrategy);
      repeatState.setExecutionStrategyExpression(executionStrategyExpression);
      repeatState.setRepeatTransitionStateName(repeatTransitionStateName);
      return repeatState;
    }
  }
}

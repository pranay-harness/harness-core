/**
 *
 */

package software.wings.sm.states;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.WingsBootstrap;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.RepeatElementType;
import software.wings.sm.Repeatable;
import software.wings.sm.SmInstance;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;

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

  private RepeatElementType repeatElementType;
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
    return execute(
        (ExecutionContextImpl) context, repeatStateExecutionData, WingsBootstrap.lookup(StateMachineExecutor.class));
  }

  ExecutionResponse execute(ExecutionContextImpl context, RepeatStateExecutionData repeatStateExecutionData,
      StateMachineExecutor stateMachineExecutor) {
    if (repeatStateExecutionData == null) {
      repeatStateExecutionData = new RepeatStateExecutionData();
    }
    List<Repeatable> repeatElements = repeatStateExecutionData.getRepeatElements();
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

    SmInstance smInstance = context.getSmInstance();
    List<String> correlationIds = new ArrayList<>();

    if (repeatStrategy == RepeatStrategy.PARALLEL) {
      for (Repeatable repeatElement : repeatElements) {
        ExecutionContextImpl contextClone = SerializationUtils.clone(context);
        contextClone.pushContextElement(repeatElement);
        String notifyId = smInstance.getUuid() + "-repeat-" + repeatElement.getName();
        stateMachineExecutor.execute(
            smInstance.getStateMachineId(), repeatTransitionStateName, contextClone, smInstance.getUuid(), notifyId);
        correlationIds.add(notifyId);
      }
    } else {
      Integer repeatElementIndex = 0;
      repeatStateExecutionData.setRepeatElementIndex(0);
      Repeatable repeatElement = repeatElements.get(repeatElementIndex);
      ExecutionContextImpl contextClone = SerializationUtils.clone(context);
      contextClone.pushContextElement(repeatElement);
      String notifyId = smInstance.getUuid() + "-repeat-" + repeatElement.getName();
      stateMachineExecutor.execute(
          smInstance.getStateMachineId(), repeatTransitionStateName, contextClone, smInstance.getUuid(), notifyId);
      correlationIds.add(notifyId);
    }

    ExecutionResponse executionResponse = new ExecutionResponse();
    executionResponse.setAsynch(true);
    executionResponse.setCorrelationIds(correlationIds);
    executionResponse.setStateExecutionData(repeatStateExecutionData);
    return executionResponse;
  }

  @Override
  public ExecutionResponse handleAsynchResponse(
      ExecutionContextImpl context, Map<String, ? extends Serializable> response) {
    return handleAsynchResponse(context, response, WingsBootstrap.lookup(StateMachineExecutor.class));
  }

  ExecutionResponse handleAsynchResponse(ExecutionContextImpl context, Map<String, ? extends Serializable> response,
      StateMachineExecutor stateMachineExecutor) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    for (Serializable status : response.values()) {
      executionStatus = (ExecutionStatus) status;
      if (executionStatus != ExecutionStatus.SUCCESS) {
        break;
      }
    }

    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) context.getStateExecutionData();
    List<Repeatable> repeatElements = repeatStateExecutionData.getRepeatElements();

    if (repeatStrategy == RepeatStrategy.PARALLEL || executionStatus == ExecutionStatus.FAILED
        || repeatStateExecutionData.indexReachedMax()) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(executionStatus);
      return executionResponse;
    } else {
      Integer repeatElementIndex = repeatStateExecutionData.getRepeatElementIndex();
      repeatElementIndex++;
      repeatStateExecutionData.setRepeatElementIndex(repeatElementIndex);
      Repeatable repeatElement = repeatElements.get(repeatElementIndex);
      ExecutionContextImpl contextClone = SerializationUtils.clone(context);
      contextClone.pushContextElement(repeatElement);

      SmInstance smInstance = context.getSmInstance();
      String notifyId = smInstance.getUuid() + "-repeat-" + repeatElement.getName();
      stateMachineExecutor.execute(
          smInstance.getStateMachineId(), repeatTransitionStateName, contextClone, smInstance.getUuid(), notifyId);
      List<String> correlationIds = new ArrayList<>();
      correlationIds.add(notifyId);

      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setAsynch(true);
      executionResponse.setCorrelationIds(correlationIds);
      return executionResponse;
    }
  }

  public RepeatElementType getRepeatElementType() {
    return repeatElementType;
  }

  public void setRepeatElementType(RepeatElementType repeatElementType) {
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
    private List<Repeatable> repeatElements = new ArrayList<>();
    private Integer repeatElementIndex;

    public List<Repeatable> getRepeatElements() {
      return repeatElements;
    }

    public void setRepeatElements(List<Repeatable> repeatElements) {
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

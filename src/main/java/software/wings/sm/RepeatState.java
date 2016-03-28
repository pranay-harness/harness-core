/**
 *
 */
package software.wings.sm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.app.WingsBootstrap;

/**
 * @author Rishi
 *
 */
public class RepeatState extends State {
  private static final long serialVersionUID = 1L;
  private static final String REPEAT_ELEMENT_INDEX = "repeatElementIndex";

  public enum RepeatStrategy { SERIAL, PARALLEL }
  ;

  private RepeatElementType repeatElementType;
  private String repeatElementExpression;
  private RepeatStrategy repeatStrategy;

  private String repeatTransitionStateName;

  private List<RepeatElement> repeatElements = new ArrayList<>();

  public RepeatState(String name) {
    super(name, StateType.REPEAT);
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      if (repeatElements == null || repeatElements.size() == 0) {
        if (repeatElementExpression != null) {
          repeatElements = context.evaluateRepeatExpression(repeatElementType, repeatElementExpression);
        }
      }
    } catch (Exception e) {
      logger.error("Error in getting repeat elements", e);
    }

    if (repeatElements == null || repeatElements.size() == 0) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      executionResponse.setErrorMessage(
          "No repeat elements found for the expression - repeatElementExpression:" + repeatElementExpression);
      return executionResponse;
    }

    context.setParam("repeatElements", (Serializable) repeatElements);

    SMInstance smInstance = context.getSmInstance();
    List<String> correlationIds = new ArrayList<>();

    if (repeatStrategy == RepeatStrategy.PARALLEL) {
      for (RepeatElement repeatElement : repeatElements) {
        context.getRepeatElementMap().put(repeatElementType, repeatElement);
        String notifyId = smInstance.getUuid() + "-repeat-" + repeatElement.getRepeatElementName();
        WingsBootstrap.lookup(StateMachineExecutor.class)
            .execute(
                smInstance.getStateMachineId(), repeatTransitionStateName, context, smInstance.getUuid(), notifyId);
        correlationIds.add(notifyId);
      }
    } else {
      Integer repeatElementIndex = 0;
      context.setParam(REPEAT_ELEMENT_INDEX, repeatElementIndex);
      RepeatElement repeatElement = repeatElements.get(repeatElementIndex);
      context.getRepeatElementMap().put(repeatElementType, repeatElement);
      String notifyId = smInstance.getUuid() + "-repeat-" + repeatElement.getRepeatElementName();
      WingsBootstrap.lookup(StateMachineExecutor.class)
          .execute(smInstance.getStateMachineId(), repeatTransitionStateName, context, smInstance.getUuid(), notifyId);
      correlationIds.add(notifyId);
    }

    ExecutionResponse executionResponse = new ExecutionResponse();
    executionResponse.setAsynch(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  @Override
  public ExecutionResponse handleAsynchResponse(
      ExecutionContext context, Map<String, ? extends Serializable> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    for (Serializable status : response.values()) {
      executionStatus = (ExecutionStatus) status;
      if (executionStatus != ExecutionStatus.SUCCESS) {
        break;
      }
    }
    if (repeatStrategy == RepeatStrategy.PARALLEL || executionStatus == ExecutionStatus.FAILED
        || (context.getParams().get(REPEAT_ELEMENT_INDEX) != null
               && (Integer) context.getParams().get(REPEAT_ELEMENT_INDEX) == repeatElements.size())) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      executionResponse.setExecutionStatus(executionStatus);
      return executionResponse;
    } else {
      SMInstance smInstance = context.getSmInstance();
      Integer repeatElementIndex = (Integer) context.getParams().get(REPEAT_ELEMENT_INDEX);
      repeatElementIndex++;
      context.setParam(REPEAT_ELEMENT_INDEX, repeatElementIndex);
      RepeatElement repeatElement = repeatElements.get(repeatElementIndex);
      context.getRepeatElementMap().put(repeatElementType, repeatElement);
      String notifyId = smInstance.getUuid() + "-repeat-" + repeatElement.getRepeatElementName();
      WingsBootstrap.lookup(StateMachineExecutor.class)
          .execute(smInstance.getStateMachineId(), repeatTransitionStateName, context, smInstance.getUuid(), notifyId);
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

  public List<RepeatElement> getRepeatElements() {
    return repeatElements;
  }

  public void setRepeatElements(List<RepeatElement> repeatElements) {
    this.repeatElements = repeatElements;
  }

  public String getRepeatTransitionStateName() {
    return repeatTransitionStateName;
  }

  public void setRepeatTransitionStateName(String repeatTransitionStateName) {
    this.repeatTransitionStateName = repeatTransitionStateName;
  }

  private static final Logger logger = LoggerFactory.getLogger(RepeatState.class);
}

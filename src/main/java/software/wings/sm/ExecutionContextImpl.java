package software.wings.sm;

import software.wings.utils.ExpressionEvaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes execution context for a state machine execution.
 *
 * @author Rishi
 */
public class ExecutionContextImpl implements ExecutionContext {
  private ExpressionEvaluator evaluator;
  private StateMachine stateMachine;
  private StateExecutionInstance stateExecutionInstance;

  public ExecutionContextImpl(
      StateExecutionInstance stateExecutionInstance, StateMachine stateMachine, ExpressionEvaluator evaluator) {
    super();
    this.stateExecutionInstance = stateExecutionInstance;
    this.stateMachine = stateMachine;
    this.evaluator = evaluator;
  }

  public StateMachine getStateMachine() {
    return stateMachine;
  }

  public void setStateMachine(StateMachine stateMachine) {
    this.stateMachine = stateMachine;
  }

  public StateExecutionInstance getStateExecutionInstance() {
    return stateExecutionInstance;
  }

  public void setStateExecutionInstance(StateExecutionInstance stateExecutionInstance) {
    this.stateExecutionInstance = stateExecutionInstance;
  }

  @Override
  public String renderExpression(String expression) {
    Map<String, Object> context = prepareContext();
    return renderExpression(expression, context);
  }

  @Override
  public String renderExpression(String expression, StateExecutionData stateExecutionData) {
    Map<String, Object> context = prepareContext(stateExecutionData);
    return renderExpression(expression, context);
  }

  private String renderExpression(String expression, Map<String, Object> context) {
    return evaluator.merge(expression, context, stateExecutionInstance.getStateName());
  }

  @Override
  public Object evaluateExpression(String expression) {
    Map<String, Object> context = prepareContext();
    return evaluateExpression(expression, context);
  }

  @Override
  public Object evaluateExpression(String expression, StateExecutionData stateExecutionData) {
    Map<String, Object> context = prepareContext(stateExecutionData);
    return evaluateExpression(expression, context);
  }

  private Object evaluateExpression(String expression, Map<String, Object> context) {
    return evaluator.evaluate(expression, context, stateExecutionInstance.getStateName());
  }

  public List<ContextElement> evaluateRepeatExpression(
      ContextElementType repeatElementType, String repeatElementExpression) {
    return (List<ContextElement>) evaluateExpression(repeatElementExpression, prepareContext());
  }

  private Map<String, Object> prepareContext(StateExecutionData stateExecutionData) {
    Map<String, Object> context = prepareContext();
    context.put(getStateExecutionInstance().getStateName(), stateExecutionData);
    return context;
  }

  private Map<String, Object> prepareContext() {
    Map<String, Object> context = new HashMap<>();
    return prepareContext(context);
  }

  private Map<String, Object> prepareContext(Map<String, Object> context) {
    // add state execution data
    context.putAll(stateExecutionInstance.getStateExecutionMap());

    // add context params
    for (ContextElement contextElement : stateExecutionInstance.getContextElements()) {
      context.putAll(contextElement.paramMap());
    }

    return context;
  }

  @Override
  public StateExecutionData getStateExecutionData() {
    return stateExecutionInstance.getStateExecutionMap().get(stateExecutionInstance.getStateName());
  }

  /**
   * @param contextElement
   */
  public void pushContextElement(ContextElement contextElement) {
    stateExecutionInstance.getContextElements().push(contextElement);
  }
}

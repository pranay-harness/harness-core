package software.wings.sm;

import software.wings.app.WingsBootstrap;
import software.wings.utils.ExpressionEvaluator;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes execution context for a state machine execution.
 *
 * @author Rishi
 */
public class ExecutionContextImpl implements ExecutionContext, Serializable {
  private static final long serialVersionUID = 1L;
  private String stateMachineId;
  private ExecutionStandardParams standardParams;
  private Deque<Repeatable> contextElements = new ArrayDeque<>();
  private Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();

  private transient StateExecutionInstance stateExecutionInstance;

  private boolean dirty = false;

  private transient ExpressionEvaluator evaluator;

  public Deque<Repeatable> getContextElements() {
    return contextElements;
  }

  public void setContextElements(Deque<Repeatable> contextElements) {
    this.contextElements = contextElements;
    dirty = true;
  }

  public void pushContextElement(Repeatable repeatElement) {
    contextElements.push(repeatElement);
    dirty = true;
  }

  public Repeatable popContextElement() {
    dirty = true;
    return contextElements.pop();
  }

  public Repeatable peekContextElement() {
    return contextElements.peek();
  }

  public String getStateMachineId() {
    return stateMachineId;
  }

  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  public Map<String, StateExecutionData> getStateExecutionMap() {
    return stateExecutionMap;
  }

  public void setStateExecutionMap(Map<String, StateExecutionData> stateExecutionMap) {
    this.stateExecutionMap = stateExecutionMap;
    dirty = true;
  }

  @Override
  public StateExecutionData getStateExecutionData() {
    return stateExecutionMap.get(stateExecutionInstance.getStateName());
  }

  public StateExecutionInstance getSmInstance() {
    return stateExecutionInstance;
  }

  public void setSmInstance(StateExecutionInstance stateExecutionInstance) {
    this.stateExecutionInstance = stateExecutionInstance;
  }

  public ExecutionStandardParams getStandardParams() {
    return standardParams;
  }

  public void setStandardParams(ExecutionStandardParams standardParams) {
    this.standardParams = standardParams;
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
    return getEvaluator().merge(expression, context, stateExecutionInstance.getStateName());
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
    return getEvaluator().evaluate(expression, context, stateExecutionInstance.getStateName());
  }

  private Map<String, Object> prepareContext(StateExecutionData stateExecutionData) {
    Map<String, Object> context = prepareContext();
    context.put(getSmInstance().getStateName(), stateExecutionData);
    return context;
  }

  private Map<String, Object> prepareContext() {
    Map<String, Object> context = new HashMap<>();
    return prepareContext(context);
  }

  private Map<String, Object> prepareContext(Map<String, Object> context) {
    // add state execution data
    context.putAll(stateExecutionMap);

    // add context params
    context.putAll(prepareContextParams());

    // add standard params
    if (standardParams != null) {
      context.putAll(standardParams.paramMap());
    }

    return context;
  }

  private Map<String, Object> prepareContextParams() {
    Map<String, Object> map = new HashMap<>();
    for (Repeatable repeatable : contextElements) {
      map.put(repeatable.getRepeatElementType().getDisplayName(), repeatable);
    }
    return map;
  }

  @Override
  public <T> T evaluateExpression(String expression, Class<T> cls, StateExecutionData stateExecutionData) {
    return (T) evaluateExpression(expression, stateExecutionData);
  }

  @Override
  public <T> T evaluateExpression(String expression, Class<T> cls) {
    return (T) evaluateExpression(expression);
  }

  public List<Repeatable> evaluateRepeatExpression(
      RepeatElementType repeatElementType, String repeatElementExpression) {
    return (List<Repeatable>) evaluateExpression(repeatElementExpression, prepareContext());
  }

  public boolean isDirty() {
    return dirty;
  }

  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  public ExpressionEvaluator getEvaluator() {
    if (evaluator == null) {
      evaluator = WingsBootstrap.lookup(ExpressionEvaluator.class);
    }
    return evaluator;
  }

  public void setEvaluator(ExpressionEvaluator evaluator) {
    this.evaluator = evaluator;
  }
}

package software.wings.sm;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.utils.ExpressionEvaluator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes execution context for a state machine execution.
 *
 * @author Rishi
 */
public class ExecutionContextImpl implements ExecutionContext {
  private static Pattern nameWindCharPattern = Pattern.compile("[-|+|*|/|\\\\| |&|$|\"|'|\\.|\\|]");
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private ExpressionEvaluator evaluator;
  @Inject private ExpressionProcessorFactory expressionProcessorFactory;
  private StateMachine stateMachine;
  private StateExecutionInstance stateExecutionInstance;

  /**
   * Instantiates a new execution context impl.
   *
   * @param stateExecutionInstance the state execution instance
   */
  public ExecutionContextImpl(StateExecutionInstance stateExecutionInstance) {
    this.stateExecutionInstance = stateExecutionInstance;
  }

  /**
   * Instantiates a new execution context impl.
   *
   * @param stateExecutionInstance the state execution instance
   * @param stateMachine           the state machine
   * @param injector               the injector
   */
  public ExecutionContextImpl(
      StateExecutionInstance stateExecutionInstance, StateMachine stateMachine, Injector injector) {
    injector.injectMembers(this);
    this.stateExecutionInstance = stateExecutionInstance;
    this.stateMachine = stateMachine;
    if (!isEmpty(stateExecutionInstance.getContextElements())) {
      stateExecutionInstance.getContextElements().forEach(contextElement -> injector.injectMembers(contextElement));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderExpression(String expression) {
    Map<String, Object> context = prepareContext();
    return renderExpression(expression, context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderExpression(String expression, StateExecutionData stateExecutionData) {
    Map<String, Object> context = prepareContext(stateExecutionData);
    return renderExpression(expression, context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object evaluateExpression(String expression) {
    Map<String, Object> context = prepareContext();
    return evaluateExpression(expression, context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object evaluateExpression(String expression, StateExecutionData stateExecutionData) {
    Map<String, Object> context = prepareContext(stateExecutionData);
    return evaluateExpression(expression, context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateExecutionData getStateExecutionData() {
    return stateExecutionInstance.getStateExecutionMap().get(stateExecutionInstance.getStateName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends ContextElement> T getContextElement(ContextElementType contextElementType) {
    ArrayDeque<ContextElement> contextElements = stateExecutionInstance.getContextElements();
    for (ContextElement contextElement : contextElements) {
      if (contextElement.getElementType() == contextElementType) {
        return (T) contextElement;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends ContextElement> List<T> getContextElementList(ContextElementType contextElementType) {
    ArrayDeque<ContextElement> contextElements = stateExecutionInstance.getContextElements();
    List<T> selected = new ArrayList<>();
    for (ContextElement contextElement : contextElements) {
      if (contextElement.getElementType() == contextElementType) {
        selected.add((T) contextElement);
      }
    }
    return selected;
  }

  @Override
  public Application getApp() {
    WorkflowStandardParams stdParam = getContextElement(ContextElementType.STANDARD);
    if (stdParam != null) {
      return stdParam.getApp();
    }
    return null;
  }

  @Override
  public Environment getEnv() {
    WorkflowStandardParams stdParam = getContextElement(ContextElementType.STANDARD);
    if (stdParam != null) {
      return stdParam.getEnv();
    }
    return null;
  }

  /**
   * Gets state machine.
   *
   * @return the state machine
   */
  public StateMachine getStateMachine() {
    return stateMachine;
  }

  /**
   * Gets state execution instance.
   *
   * @return the state execution instance
   */
  public StateExecutionInstance getStateExecutionInstance() {
    return stateExecutionInstance;
  }

  /**
   * Push context element.
   *
   * @param contextElement the context element
   */
  public void pushContextElement(ContextElement contextElement) {
    stateExecutionInstance.getContextElements().push(contextElement);
  }

  private String renderExpression(String expression, Map<String, Object> context) {
    return evaluator.merge(expression, context, normalizeStateName(stateExecutionInstance.getStateName()));
  }

  private Object evaluateExpression(String expression, Map<String, Object> context) {
    expression = normalizeExpression(expression, context, normalizeStateName(stateExecutionInstance.getStateName()));
    return evaluator.evaluate(expression, context);
  }

  private Map<String, Object> prepareContext(StateExecutionData stateExecutionData) {
    Map<String, Object> context = prepareContext();
    context.put(normalizeStateName(getStateExecutionInstance().getStateName()), stateExecutionData);
    return context;
  }

  private Map<String, Object> prepareContext() {
    Map<String, Object> context = new HashMap<>();
    return prepareContext(context);
  }

  private String normalizeStateName(String name) {
    Matcher matcher = nameWindCharPattern.matcher(name);
    return matcher.replaceAll("__");
  }

  private Map<String, Object> prepareContext(Map<String, Object> context) {
    // add state execution data
    context.putAll(stateExecutionInstance.getStateExecutionMap());

    // add context params
    Iterator<ContextElement> it = stateExecutionInstance.getContextElements().descendingIterator();
    while (it.hasNext()) {
      ContextElement contextElement = it.next();

      Map<String, Object> map = contextElement.paramMap();
      if (map != null) {
        context.putAll(map);
      }
    }

    return context;
  }

  private String normalizeExpression(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    List<ExpressionProcessor> expressionProcessors = new ArrayList<>();
    Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(expression);

    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String variable = matcher.group(0);
      logger.debug("wingsVariable found: {}", variable);

      // remove $ and braces(${varName})
      variable = variable.substring(2, variable.length() - 1);

      String topObjectName = variable;
      if (topObjectName.indexOf('.') > 0) {
        topObjectName = topObjectName.substring(0, topObjectName.indexOf('.'));
      }

      boolean unknownObject = false;
      if (!context.containsKey(topObjectName)) {
        unknownObject = true;
      }
      if (unknownObject) {
        for (ExpressionProcessor expressionProcessor : expressionProcessors) {
          String newVariable = expressionProcessor.normalizeExpression(variable);
          if (newVariable != null) {
            variable = newVariable;
            unknownObject = false;
            break;
          }
        }
      }
      if (unknownObject) {
        ExpressionProcessor expressionProcessor = expressionProcessorFactory.getExpressionProcessor(variable, this);
        if (expressionProcessor != null) {
          variable = expressionProcessor.normalizeExpression(variable);
          expressionProcessors.add(expressionProcessor);
          unknownObject = false;
        }
      }
      if (unknownObject) {
        variable = defaultObjectPrefix + "." + variable;
      }

      matcher.appendReplacement(sb, variable);
    }
    matcher.appendTail(sb);

    for (ExpressionProcessor expressionProcessor : expressionProcessors) {
      context.put(expressionProcessor.getPrefixObjectName(), expressionProcessor);
    }

    return sb.toString();
  }

  @Override
  public String getWorkflowExecutionId() {
    return stateExecutionInstance.getExecutionUuid();
  }
}

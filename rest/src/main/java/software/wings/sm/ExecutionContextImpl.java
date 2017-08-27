package software.wings.sm;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.WorkflowType;
import software.wings.common.VariableProcessor;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.utils.ExpressionEvaluator;

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
  private static final String CURRENT_STATE = "currentState";
  private static Pattern nameWindCharPattern = Pattern.compile("[+|*|/|\\\\| |&|$|\"|'|\\.|\\|]");
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private ExpressionEvaluator evaluator;
  @Inject private ExpressionProcessorFactory expressionProcessorFactory;
  @Inject private VariableProcessor variableProcessor;
  @Inject @Transient private SettingsService settingsService;
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
      stateExecutionInstance.getContextElements().forEach(contextElement -> {
        injector.injectMembers(contextElement);
        if (contextElement instanceof ExecutionContextAware) {
          ((ExecutionContextAware) contextElement).setExecutionContext(this);
        }

      });
    }
    if (!isEmpty(stateExecutionInstance.getExecutionEventAdvisors())) {
      stateExecutionInstance.getExecutionEventAdvisors().forEach(advisor -> { injector.injectMembers(advisor); });
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
  public Object evaluateExpression(String expression, Object stateExecutionData) {
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

  public StateExecutionData getStateExecutionData(String stateName) {
    return stateExecutionInstance.getStateExecutionMap().get(stateName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends ContextElement> T getContextElement() {
    return (T) stateExecutionInstance.getContextElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends ContextElement> T getContextElement(ContextElementType contextElementType) {
    return (T) stateExecutionInstance.getContextElements()
        .stream()
        .filter(contextElement -> contextElement.getElementType() == contextElementType)
        .findFirst()
        .orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends ContextElement> T getContextElement(ContextElementType contextElementType, String name) {
    return (T) stateExecutionInstance.getContextElements()
        .stream()
        .filter(contextElement
            -> contextElement.getElementType() == contextElementType && name.equals(contextElement.getName()))
        .findFirst()
        .orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends ContextElement> List<T> getContextElementList(ContextElementType contextElementType) {
    return stateExecutionInstance.getContextElements()
        .stream()
        .filter(contextElement -> contextElement.getElementType() == contextElementType)
        .map(contextElement -> (T) contextElement)
        .collect(toList());
  }

  public Application getApp() {
    WorkflowStandardParams stdParam = getContextElement(ContextElementType.STANDARD);
    if (stdParam != null) {
      return stdParam.getApp();
    }
    return null;
  }

  public Environment getEnv() {
    WorkflowStandardParams stdParam = getContextElement(ContextElementType.STANDARD);
    if (stdParam != null) {
      return stdParam.getEnv();
    }
    return null;
  }

  @Override
  public ErrorStrategy getErrorStrategy() {
    WorkflowStandardParams stdParam = getContextElement(ContextElementType.STANDARD);
    if (stdParam != null) {
      return stdParam.getErrorStrategy();
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
   * Sets state execution instance.
   *
   * @param stateExecutionInstance the state execution instance
   */
  void setStateExecutionInstance(StateExecutionInstance stateExecutionInstance) {
    this.stateExecutionInstance = stateExecutionInstance;
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

  private Map<String, Object> prepareContext(Object stateExecutionData) {
    Map<String, Object> context = prepareContext();
    if (stateExecutionData != null) {
      context.put(normalizeStateName(getStateExecutionInstance().getStateName()), stateExecutionData);
    }
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
    stateExecutionInstance.getStateExecutionMap().entrySet().forEach(
        entry -> { context.put(normalizeStateName(entry.getKey()), entry.getValue()); });

    context.put(CURRENT_STATE, normalizeStateName(getStateExecutionInstance().getStateName()));

    // add context params
    Iterator<ContextElement> it = stateExecutionInstance.getContextElements().descendingIterator();
    while (it.hasNext()) {
      ContextElement contextElement = it.next();

      Map<String, Object> map = contextElement.paramMap(this);
      if (map != null) {
        context.putAll(map);
      }
    }

    context.putAll(variableProcessor.getVariables(stateExecutionInstance.getContextElements()));

    return context;
  }

  private String normalizeExpression(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return null;
    }
    List<ExpressionProcessor> expressionProcessors = new ArrayList<>();
    Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(expression);

    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String variable = matcher.group(0);
      logger.debug("wingsVariable found: {}", variable);

      // remove $ and braces(${varName})
      variable = variable.substring(2, variable.length() - 1);

      String topObjectName = variable;
      String topObjectNameSuffix = null;
      int ind = variable.indexOf('.');
      if (ind > 0) {
        topObjectName = normalizeStateName(variable.substring(0, ind));
        topObjectNameSuffix = variable.substring(ind);
        variable = topObjectName + topObjectNameSuffix;
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

  @Override
  public String getWorkflowExecutionName() {
    return stateExecutionInstance.getExecutionName();
  }

  @Override
  public WorkflowType getWorkflowType() {
    return stateExecutionInstance.getExecutionType();
  }

  @Override
  public String getStateExecutionInstanceId() {
    return stateExecutionInstance.getUuid();
  }

  @Override
  public String getAppId() {
    return ((WorkflowStandardParams) getContextElement(ContextElementType.STANDARD)).getAppId();
  }

  @Override
  public String getStateExecutionInstanceName() {
    return stateExecutionInstance.getStateName();
  }

  @Override
  public Map<String, String> getServiceVariables() {
    return variableProcessor.getVariables(stateExecutionInstance.getContextElements());
  }

  @Override
  public SettingValue getSettingValue(String id, String type) {
    return settingsService.getSettingAttributesByType(getEnv().getAppId(), getEnv().getUuid(), type)
        .stream()
        .filter(settingAttribute -> StringUtils.equals(settingAttribute.getUuid(), id))
        .findFirst()
        .map(settingAttribute -> settingAttribute.getValue())
        .orElse(null);
  }
}

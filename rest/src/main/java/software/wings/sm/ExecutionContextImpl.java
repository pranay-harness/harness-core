package software.wings.sm;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static software.wings.sm.ContextElement.SAFE_DISPLAY_SERVICE_VARIABLE;
import static software.wings.sm.ContextElement.SERVICE_VARIABLE;

import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceArtifactElement;
import software.wings.beans.Application;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.common.VariableProcessor;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.utils.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Describes execution context for a state machine execution.
 *
 * @author Rishi
 */
public class ExecutionContextImpl implements DeploymentExecutionContext {
  private static final Pattern wildCharPattern = Pattern.compile("[+|*|/|\\\\| |&|$|\"|'|\\.|\\|]");
  private static final Pattern argsCharPattern = Pattern.compile("[(|)|\"|\']");
  private static final Logger logger = LoggerFactory.getLogger(ExecutionContextImpl.class);
  @Inject @Transient private ExpressionEvaluator evaluator;
  @Inject @Transient private ExpressionProcessorFactory expressionProcessorFactory;
  @Inject @Transient private VariableProcessor variableProcessor;
  @Inject @Transient private SettingsService settingsService;
  @Inject @Transient private ServiceTemplateService serviceTemplateService;
  @Inject @Transient private ArtifactService artifactService;
  private StateMachine stateMachine;
  private StateExecutionInstance stateExecutionInstance;
  @Transient private transient Map<String, Object> contextMap;

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
      stateExecutionInstance.getExecutionEventAdvisors().forEach(injector::injectMembers);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Artifact> getArtifacts() {
    WorkflowStandardParams workflowStandardParams =
        (WorkflowStandardParams) getContextElement(ContextElementType.STANDARD);
    List<ContextElement> contextElementList = getContextElementList(ContextElementType.ARTIFACT);
    if (isEmpty(contextElementList)) {
      return workflowStandardParams.getArtifacts();
    }
    List<Artifact> list = new ArrayList<>();
    for (ContextElement contextElement : contextElementList) {
      list.add(artifactService.get(workflowStandardParams.getAppId(), contextElement.getUuid()));
    }
    return list;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Artifact getArtifactForService(String serviceId) {
    WorkflowStandardParams workflowStandardParams =
        (WorkflowStandardParams) getContextElement(ContextElementType.STANDARD);
    List<ContextElement> contextElementList = getContextElementList(ContextElementType.ARTIFACT);
    if (contextElementList == null) {
      return workflowStandardParams.getArtifactForService(serviceId);
    }
    Optional<ContextElement> contextElementOptional =
        contextElementList.stream()
            .filter(art
                -> ((ServiceArtifactElement) art).getServiceIds() != null
                    && ((ServiceArtifactElement) art).getServiceIds().contains(serviceId))
            .findFirst();

    if (contextElementOptional.isPresent()) {
      return artifactService.get(workflowStandardParams.getAppId(), contextElementOptional.get().getUuid());
    } else {
      return workflowStandardParams.getArtifactForService(serviceId);
    }
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
    return normalizeAndEvaluate(expression, context, normalizeStateName(stateExecutionInstance.getStateName()));
  }

  private Object normalizeAndEvaluate(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return null;
    }
    List<ExpressionProcessor> expressionProcessors = new ArrayList<>();
    Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(expression);

    StringBuffer sb = new StringBuffer();

    String varPrefix = "VAR_";
    Map<String, String> normalizedExpressionMap = new HashMap<>();

    while (matcher.find()) {
      String variable = matcher.group(0);
      logger.debug("wingsVariable found: {}", variable);

      // remove $ and braces(${varName})
      variable = variable.substring(2, variable.length() - 1);

      String topObjectName = variable;
      String topObjectNameSuffix = null;
      int ind = variable.indexOf('.');
      if (ind > 0) {
        String firstPart = variable.substring(0, ind);
        if (!argsCharPattern.matcher(firstPart).find()) {
          topObjectName = normalizeStateName(firstPart);
          topObjectNameSuffix = variable.substring(ind);
          variable = topObjectName + topObjectNameSuffix;
        }
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

      String varId = varPrefix + new Random().nextInt(10000);
      while (normalizedExpressionMap.containsKey(varId)) {
        varId = varPrefix + new Random().nextInt(10000);
      }
      normalizedExpressionMap.put(varId, variable);
      matcher.appendReplacement(sb, varId);
    }
    matcher.appendTail(sb);

    for (ExpressionProcessor expressionProcessor : expressionProcessors) {
      context.put(expressionProcessor.getPrefixObjectName(), expressionProcessor);
    }

    return evaluate(sb.toString(), normalizedExpressionMap, context, defaultObjectPrefix);
  }

  private Object evaluate(String expr, Map<String, String> normalizedExpressionMap, Map<String, Object> context,
      String defaultObjectPrefix) {
    Map<String, Object> evaluatedValueMap = new HashMap<>();
    for (String key : normalizedExpressionMap.keySet()) {
      Object val = evaluator.evaluate(normalizedExpressionMap.get(key), context);
      if (val instanceof String) {
        String valStr = (String) val;
        Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(valStr);
        if (matcher.find()) {
          val = normalizeAndEvaluate(valStr, context, defaultObjectPrefix);
        }
      }
      evaluatedValueMap.put(key, val);
    }

    logger.info("expr: {}, evaluatedValueMap: {}", expr, evaluatedValueMap);
    return evaluator.evaluate(expr, evaluatedValueMap);
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
    if (contextMap == null) {
      contextMap = prepareContext(context);
    }
    return contextMap;
  }

  private String normalizeStateName(String name) {
    Matcher matcher = wildCharPattern.matcher(name);
    return matcher.replaceAll("__");
  }

  private Map<String, Object> prepareContext(Map<String, Object> context) {
    // add state execution data
    stateExecutionInstance.getStateExecutionMap().forEach((key, value) -> context.put(normalizeStateName(key), value));

    // add context params
    Iterator<ContextElement> it = stateExecutionInstance.getContextElements().descendingIterator();
    while (it.hasNext()) {
      ContextElement contextElement = it.next();

      Map<String, Object> map = contextElement.paramMap(this);
      if (map != null) {
        context.putAll(map);
      }
    }

    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    if (phaseElement != null && phaseElement.getVariableOverrides() != null
        && !phaseElement.getVariableOverrides().isEmpty()) {
      Map<String, String> map = (Map<String, String>) context.get(ContextElement.SERVICE_VARIABLE);
      if (map == null) {
        map = new HashMap<>();
      }
      map.putAll(phaseElement.getVariableOverrides().stream().collect(
          Collectors.toMap(nv -> nv.getName(), nv -> nv.getValue())));
      context.put(ContextElement.SERVICE_VARIABLE, map);
    }

    context.putAll(
        variableProcessor.getVariables(stateExecutionInstance.getContextElements(), getWorkflowExecutionId()));

    return context;
  }

  @Override
  public String getWorkflowExecutionId() {
    return stateExecutionInstance.getExecutionUuid();
  }

  @Override
  public String getWorkflowId() {
    return stateExecutionInstance.getWorkflowId();
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
  public OrchestrationWorkflowType getOrchestrationWorkflowType() {
    return stateExecutionInstance.getOrchestrationWorkflowType();
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
    return getServiceVariables(false);
  }

  @Override
  public Map<String, String> getSafeDisplayServiceVariables() {
    return getServiceVariables(true);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getServiceVariables(boolean maskEncryptedFields) {
    if (contextMap != null) {
      return (Map<String, String>) contextMap.get(
          maskEncryptedFields ? SAFE_DISPLAY_SERVICE_VARIABLE : SERVICE_VARIABLE);
    }

    Map<String, String> variables = new HashMap<>();
    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    if (phaseElement == null || phaseElement.getServiceElement() == null
        || phaseElement.getServiceElement().getUuid() == null) {
      return variables;
    }
    String envId = getEnv().getUuid();
    Optional<Key<ServiceTemplate>> serviceTemplateKey =
        serviceTemplateService
            .getTemplateRefKeysByService(getAppId(), phaseElement.getServiceElement().getUuid(), envId)
            .stream()
            .findFirst();
    if (!serviceTemplateKey.isPresent()) {
      return variables;
    }
    ServiceTemplate serviceTemplate = serviceTemplateService.get(getAppId(), (String) serviceTemplateKey.get().getId());
    List<ServiceVariable> serviceVariables = serviceTemplateService.computeServiceVariables(
        getAppId(), envId, serviceTemplate.getUuid(), getWorkflowExecutionId(), maskEncryptedFields);
    serviceVariables.forEach(serviceVariable
        -> variables.put(
            renderExpression(serviceVariable.getName()), renderExpression(new String(serviceVariable.getValue()))));

    return variables;
  }

  @Override
  public SettingValue getSettingValue(String id, String type) {
    return settingsService.getSettingAttributesByType(getEnv().getAppId(), getEnv().getUuid(), type)
        .stream()
        .filter(settingAttribute -> StringUtils.equals(settingAttribute.getUuid(), id))
        .findFirst()
        .map(SettingAttribute::getValue)
        .orElse(null);
  }

  @Override
  public SettingValue getGlobalSettingValue(String accountId, String settingId, String type) {
    return settingsService.getGlobalSettingAttributesByType(accountId, type)
        .stream()
        .filter(settingAttribute -> StringUtils.equals(settingAttribute.getUuid(), settingId))
        .findFirst()
        .map(SettingAttribute::getValue)
        .orElse(null);
  }
}

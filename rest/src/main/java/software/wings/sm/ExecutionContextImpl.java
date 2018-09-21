package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME_VARIABLE;
import static software.wings.sm.ContextElement.ARTIFACT;
import static software.wings.sm.ContextElement.SAFE_DISPLAY_SERVICE_VARIABLE;
import static software.wings.sm.ContextElement.SERVICE_VARIABLE;

import com.google.inject.Inject;
import com.google.inject.Injector;

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
import software.wings.beans.NameValuePair;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SweepingOutput;
import software.wings.beans.SweepingOutput.Scope;
import software.wings.beans.SweepingOutput.SweepingOutputBuilder;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.common.VariableProcessor;
import software.wings.expression.ExpressionEvaluator;
import software.wings.expression.SweepingOutputFunctor;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  private static final Pattern wildCharPattern = Pattern.compile("[+|*|/|\\\\| |&|$|\"|'|.|\\|]");
  private static final Pattern argsCharPattern = Pattern.compile("[(|)|\"|\']");
  private static final Logger logger = LoggerFactory.getLogger(ExecutionContextImpl.class);

  @Inject @Transient private ExpressionEvaluator evaluator;
  @Inject @Transient private ExpressionProcessorFactory expressionProcessorFactory;
  @Inject @Transient private VariableProcessor variableProcessor;
  @Inject @Transient private SettingsService settingsService;
  @Inject @Transient private ServiceTemplateService serviceTemplateService;
  @Inject @Transient private ArtifactService artifactService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient SweepingOutputService sweepingOutputService;

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
    if (isNotEmpty(stateExecutionInstance.getContextElements())) {
      stateExecutionInstance.getContextElements().forEach(contextElement -> {
        injector.injectMembers(contextElement);
        if (contextElement instanceof ExecutionContextAware) {
          ((ExecutionContextAware) contextElement).setExecutionContext(this);
        }
      });
    }
    if (isNotEmpty(stateExecutionInstance.getExecutionEventAdvisors())) {
      stateExecutionInstance.getExecutionEventAdvisors().forEach(injector::injectMembers);
    }
  }

  public static void addArtifactToContext(
      ArtifactStreamService artifactStreamService, String accountId, Map<String, Object> map, Artifact artifact) {
    if (artifact != null) {
      artifact.setSource(artifactStreamService.fetchArtifactSourceProperties(
          accountId, artifact.getAppId(), artifact.getArtifactStreamId()));
      map.put(ARTIFACT, artifact);
      String artifactFileName = null;
      if (isNotEmpty(artifact.getArtifactFiles())) {
        artifactFileName = artifact.getArtifactFiles().get(0).getName();
      } else if (artifact.getMetadata() != null) {
        artifactFileName = artifact.getArtifactFileName();
      }
      if (isNotEmpty(artifactFileName)) {
        map.put(ARTIFACT_FILE_NAME_VARIABLE, artifactFileName);
      }
    }
  }

  @Override
  public String renderExpression(String expression) {
    Map<String, Object> context = prepareContext();
    return renderExpression(expression, context);
  }

  @Override
  public String renderExpression(String expression, Object addition) {
    Map<String, Object> context;
    if (addition instanceof List) {
      List<ContextElement> contextElements = (List<ContextElement>) addition;
      context = new HashMap<>();
      context.putAll(prepareContext());
      for (ContextElement contextElement : contextElements) {
        context.putAll(contextElement.paramMap(this));
      }
    } else if (addition instanceof StateExecutionData) {
      context = prepareContext(addition);
    } else if (addition instanceof Artifact) {
      context = prepareContext();
      Artifact artifact = (Artifact) addition;
      addArtifactToContext(artifactStreamService, getApp().getAccountId(), context, artifact);
    } else {
      context = prepareContext();
    }
    return renderExpression(expression, context);
  }

  @Override
  public String renderExpression(String expression, Object stateExecutionData, Object addition) {
    Map<String, Object> context = prepareContext(stateExecutionData);
    if (addition instanceof Artifact) {
      Artifact artifact = (Artifact) addition;
      addArtifactToContext(artifactStreamService, getApp().getAccountId(), context, artifact);
    }
    return renderExpression(expression, context);
  }

  @Override
  public Object evaluateExpression(String expression) {
    Map<String, Object> context = prepareContext();
    return evaluateExpression(expression, context);
  }

  @Override
  public Object evaluateExpression(String expression, Object stateExecutionData) {
    Map<String, Object> context = prepareContext(stateExecutionData);
    return evaluateExpression(expression, context);
  }

  @Override
  public StateExecutionData getStateExecutionData() {
    return stateExecutionInstance.getStateExecutionData();
  }

  @Override
  public <T extends ContextElement> T getContextElement() {
    return (T) stateExecutionInstance.getContextElement();
  }

  @Override
  public <T extends ContextElement> T getContextElement(ContextElementType contextElementType) {
    return (T) stateExecutionInstance.getContextElements()
        .stream()
        .filter(contextElement -> contextElement.getElementType() == contextElementType)
        .findFirst()
        .orElse(null);
  }

  @Override
  public <T extends ContextElement> T getContextElement(ContextElementType contextElementType, String name) {
    return (T) stateExecutionInstance.getContextElements()
        .stream()
        .filter(contextElement
            -> contextElement.getElementType() == contextElementType && name.equals(contextElement.getName()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public <T extends ContextElement> List<T> getContextElementList(ContextElementType contextElementType) {
    return stateExecutionInstance.getContextElements()
        .stream()
        .filter(contextElement -> contextElement.getElementType() == contextElementType)
        .map(contextElement -> (T) contextElement)
        .collect(toList());
  }

  @Override
  public List<Artifact> getArtifacts() {
    WorkflowStandardParams workflowStandardParams = getContextElement(ContextElementType.STANDARD);
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

  @Override
  public Artifact getArtifactForService(String serviceId) {
    WorkflowStandardParams workflowStandardParams = getContextElement(ContextElementType.STANDARD);
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

  public String renderExpression(String expression, Map<String, Object> context) {
    return evaluator.substitute(expression, context, normalizeStateName(stateExecutionInstance.getDisplayName()));
  }

  public Object evaluateExpression(String expression, Map<String, Object> context) {
    return normalizeAndEvaluate(expression, context, normalizeStateName(stateExecutionInstance.getDisplayName()));
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
    for (Entry<String, String> entry : normalizedExpressionMap.entrySet()) {
      String key = entry.getKey();
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

    logger.debug("expr: {}, evaluatedValueMap: {}", expr, evaluatedValueMap);
    return evaluator.evaluate(expr, evaluatedValueMap);
  }

  public Map<String, Object> prepareContext(Object executionData) {
    Map<String, Object> context = prepareContext();
    if (executionData != null) {
      context.put(normalizeStateName(getStateExecutionInstance().getDisplayName()), executionData);
    }
    if (executionData instanceof StateExecutionData) {
      StateExecutionData stateExecutionData = (StateExecutionData) executionData;
      if (isNotEmpty(stateExecutionData.getTemplateVariable())) {
        context.putAll(stateExecutionData.getTemplateVariable());
      }
    }
    return context;
  }

  @Override
  public Map<String, Object> asMap() {
    Map<String, Object> context = new HashMap<>();
    context.putAll(prepareContext());
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

  @SuppressWarnings("unchecked")
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
    if (phaseElement != null && isNotEmpty(phaseElement.getVariableOverrides())) {
      Map<String, String> map = (Map<String, String>) context.get(SERVICE_VARIABLE);
      if (map == null) {
        map = new HashMap<>();
      }
      map.putAll(phaseElement.getVariableOverrides().stream().collect(
          Collectors.toMap(NameValuePair::getName, NameValuePair::getValue)));
      context.put(SERVICE_VARIABLE, map);
    }

    String workflowExecutionId = getWorkflowExecutionId();

    context.putAll(variableProcessor.getVariables(stateExecutionInstance.getContextElements(), workflowExecutionId));

    final SweepingOutput sweepingOutput = prepareSweepingOutputBuilder(null).build();

    context.put("context",
        SweepingOutputFunctor.builder()
            .sweepingOutputService(sweepingOutputService)
            .appId(sweepingOutput.getAppId())
            .pipelineExecutionId(sweepingOutput.getPipelineExecutionId())
            .workflowExecutionId(sweepingOutput.getWorkflowExecutionId())
            .phaseExecutionId(sweepingOutput.getPhaseExecutionId())
            .build());

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
  public String getPipelineStateElementId() {
    return stateExecutionInstance.getPipelineStateElementId();
  }

  @Override
  public String getAppId() {
    final ContextElement contextElement = getContextElement(ContextElementType.STANDARD);
    if (!(contextElement instanceof WorkflowStandardParams)) {
      return null;
    }
    return ((WorkflowStandardParams) contextElement).getAppId();
  }

  @Override
  public String getStateExecutionInstanceName() {
    return stateExecutionInstance.getDisplayName();
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
  public SettingValue getGlobalSettingValue(String accountId, String settingId) {
    return settingsService.getSettingValueById(accountId, settingId);
  }

  @Override
  public SweepingOutputBuilder prepareSweepingOutputBuilder(SweepingOutput.Scope sweepingOutputScope) {
    // Default scope is pipeline
    if (sweepingOutputScope == null) {
      sweepingOutputScope = Scope.PIPELINE;
    }
    WorkflowStandardParams workflowStandardParams = getContextElement(ContextElementType.STANDARD);
    String pipelineExecutionId = workflowStandardParams == null || workflowStandardParams.getWorkflowElement() == null
        ? null
        : workflowStandardParams.getWorkflowElement().getPipelineDeploymentUuid();
    String workflowExecutionId = getWorkflowExecutionId();

    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String phaseExecutionId = phaseElement == null ? null : workflowExecutionId + phaseElement.getUuid();

    if (pipelineExecutionId == null || !Scope.PIPELINE.equals(sweepingOutputScope)) {
      pipelineExecutionId = generateUuid();
    }
    if (workflowExecutionId == null || Scope.PHASE.equals(sweepingOutputScope)) {
      workflowExecutionId = generateUuid();
    }
    if (phaseExecutionId == null) {
      phaseExecutionId = generateUuid();
    }
    return SweepingOutput.builder()
        .appId(getAppId())
        .pipelineExecutionId(pipelineExecutionId)
        .workflowExecutionId(workflowExecutionId)
        .phaseExecutionId(phaseExecutionId);
  }
}

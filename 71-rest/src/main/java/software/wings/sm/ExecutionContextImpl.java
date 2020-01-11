package software.wings.sm;

import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.ServiceVariable.Type.TEXT;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.MASKED;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.sm.ContextElement.ARTIFACT;
import static software.wings.sm.ContextElement.ENVIRONMENT_VARIABLE;
import static software.wings.sm.ContextElement.SAFE_DISPLAY_SERVICE_VARIABLE;
import static software.wings.sm.ContextElement.SERVICE_VARIABLE;
import static software.wings.utils.KubernetesConvention.getNormalizedInfraMappingIdLabelValue;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceBuilder;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.LateBindingMap;
import io.harness.expression.LateBindingValue;
import io.harness.expression.SecretString;
import io.harness.expression.VariableResolverTracker;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Key;
import software.wings.api.DeploymentType;
import software.wings.api.InfraMappingElement;
import software.wings.api.InfraMappingElement.CloudProvider;
import software.wings.api.InfraMappingElement.Helm;
import software.wings.api.InfraMappingElement.InfraMappingElementBuilder;
import software.wings.api.InfraMappingElement.Kubernetes;
import software.wings.api.InfraMappingElement.Pcf;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceArtifactElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfraMappingSweepingOutput;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NameValuePair;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.common.InfrastructureConstants;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.expression.SecretFunctor;
import software.wings.expression.ShellScriptFunctor;
import software.wings.expression.SubstitutionFunctor;
import software.wings.expression.SweepingOutputFunctor;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.AppLogContext;
import software.wings.service.impl.PipelineWorkflowExecutionLogContext;
import software.wings.service.impl.StateExecutionInstanceLogContext;
import software.wings.service.impl.SweepingOutputServiceImpl;
import software.wings.service.impl.WorkflowExecutionLogContext;
import software.wings.service.impl.WorkflowLogContext;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry.SweepingOutputInquiryBuilder;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.settings.SettingValue;
import software.wings.sm.ExecutionContextImpl.ServiceVariables.ServiceVariablesBuilder;

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

@Slf4j
public class ExecutionContextImpl implements DeploymentExecutionContext {
  public static final String PHASE_PARAM = "PHASE_PARAM";

  private static final Pattern wildCharPattern = Pattern.compile("[-+*/\\\\ &$\"'.|]");
  private static final Pattern argsCharPattern = Pattern.compile("[()\"']");

  @Inject private transient ArtifactService artifactService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient ExpressionProcessorFactory expressionProcessorFactory;
  @Inject private transient ManagerDecryptionService managerDecryptionService;
  @Inject private transient ManagerExpressionEvaluator evaluator;
  @Inject private transient SecretManager secretManager;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ServiceVariableService serviceVariableService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject private transient VariableProcessor variableProcessor;
  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private transient InfrastructureDefinitionService infrastructureDefinitionService;

  private StateMachine stateMachine;
  private StateExecutionInstance stateExecutionInstance;
  private transient Map<String, Object> contextMap;
  @Getter private transient VariableResolverTracker variableResolverTracker = new VariableResolverTracker();

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
      artifact.setSource(
          artifactStreamService.fetchArtifactSourceProperties(accountId, artifact.getArtifactStreamId()));
      map.put(ARTIFACT, artifact);
      String artifactFileName = null;
      if (isNotEmpty(artifact.getArtifactFiles())) {
        artifactFileName = artifact.getArtifactFiles().get(0).getName();
      } else if (artifact.getMetadata() != null) {
        artifactFileName = artifact.getFileName();
      }
      if (isNotEmpty(artifactFileName)) {
        map.put(ExpressionEvaluator.ARTIFACT_FILE_NAME_VARIABLE, artifactFileName);
      }
    }
  }

  @Override
  public String renderExpression(String expression) {
    Map<String, Object> context = prepareContext(null);
    return renderExpression(expression, context);
  }

  @Override
  public String renderExpression(String expression, StateExecutionContext stateExecutionContext) {
    return renderExpression(expression, prepareContext(stateExecutionContext));
  }

  @Override
  public List<String> renderExpressionList(List<String> expressions) {
    return renderExpressionList(expressions, ",");
  }

  @Override
  public List<String> renderExpressionList(List<String> expressions, String separator) {
    List<String> result = null;
    if (expressions != null) {
      result = new ArrayList<>();
      for (String expression : expressions) {
        result.addAll(
            Splitter.on(separator).trimResults().omitEmptyStrings().splitToList(renderExpression(expression)));
      }
    }
    return result;
  }

  @Override
  public Object evaluateExpression(String expression) {
    return evaluateExpression(expression, null);
  }

  @Override
  public Object evaluateExpression(String expression, StateExecutionContext stateExecutionContext) {
    return normalizeAndEvaluate(expression, prepareContext(stateExecutionContext),
        asList(normalizeStateName(stateExecutionInstance.getDisplayName()), "context"));
  }

  @Override
  public StateExecutionData getStateExecutionData() {
    return stateExecutionInstance.fetchStateExecutionData();
  }

  @Override
  public <T extends ContextElement> T getContextElement() {
    return (T) stateExecutionInstance.getContextElement();
  }

  @Override
  public <T extends ContextElement> T getContextElement(ContextElementType contextElementType) {
    if (stateExecutionInstance.getContextElements() == null) {
      return null;
    }
    return (T) stateExecutionInstance.getContextElements()
        .stream()
        .filter(contextElement -> contextElement.getElementType() == contextElementType)
        .findFirst()
        .orElse(null);
  }

  @Override
  public WorkflowStandardParams fetchWorkflowStandardParamsFromContext() {
    if (stateExecutionInstance.getContextElements() == null) {
      throw new InvalidRequestException("State Execution Context Elements can not be null");
    }
    final WorkflowStandardParams workflowStandardParams =
        (WorkflowStandardParams) stateExecutionInstance.getContextElements()
            .stream()
            .filter(contextElement -> contextElement.getElementType() == ContextElementType.STANDARD)
            .findFirst()
            .orElse(null);

    if (workflowStandardParams == null) {
      throw new InvalidRequestException("Workflow Standard Params can not be null");
    }
    return workflowStandardParams;
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
    WorkflowStandardParams workflowStandardParams = fetchWorkflowStandardParamsFromContext();
    String accountId = workflowStandardParams.fetchRequiredApp().getAccountId();
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      List<ContextElement> contextElementList = getContextElementList(ContextElementType.ARTIFACT);
      if (isEmpty(contextElementList)) {
        return workflowStandardParams.getArtifacts();
      }
      List<Artifact> list = new ArrayList<>();
      for (ContextElement contextElement : contextElementList) {
        list.add(artifactService.get(contextElement.getUuid()));
      }
      return list;
    } else {
      List<ContextElement> contextElementList = getContextElementList(ContextElementType.ARTIFACT_VARIABLE);
      if (isEmpty(contextElementList)) {
        return workflowStandardParams.getArtifacts();
      }
      List<Artifact> list = new ArrayList<>();
      for (ContextElement contextElement : contextElementList) {
        list.add(artifactService.get(contextElement.getUuid()));
      }
      return list;
    }
  }

  @Override
  public Artifact getArtifactForService(String serviceId) {
    WorkflowStandardParams workflowStandardParams = fetchWorkflowStandardParamsFromContext();
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
      return artifactService.get(contextElementOptional.get().getUuid());
    } else {
      return workflowStandardParams.getArtifactForService(serviceId);
    }
  }

  private Map<String, Artifact> getArtifactMapForPhase() {
    SweepingOutputInquiry sweepingOutputInput = prepareSweepingOutputInquiryBuilder().name("artifacts").build();
    SweepingOutputInstance result = sweepingOutputService.find(sweepingOutputInput);

    if (result == null) {
      return null;
    }
    return (Map<String, Artifact>) KryoUtils.asInflatedObject(result.getOutput());
  }

  private boolean isArtifactVariableForService(String serviceId, ArtifactVariable artifactVariable) {
    switch (artifactVariable.getEntityType()) {
      case SERVICE:
        return artifactVariable.getEntityId().equals(serviceId);
      case ENVIRONMENT:
        if (isEmpty(artifactVariable.getOverriddenArtifactVariables())) {
          return true;
        }
        for (ArtifactVariable overriddenArtifactVariable : artifactVariable.getOverriddenArtifactVariables()) {
          if (isArtifactVariableForService(serviceId, overriddenArtifactVariable)) {
            return true;
          }
        }
        return false;
      case WORKFLOW:
        return true;
      default:
        return false;
    }
  }

  @Override
  public Map<String, Artifact> getArtifactsForService(String serviceId) {
    WorkflowStandardParams workflowStandardParams = fetchWorkflowStandardParamsFromContext();
    Map<String, Artifact> map = new HashMap<>();
    if (workflowStandardParams.getWorkflowElement() == null) {
      return map;
    }
    List<ArtifactVariable> artifactVariables = workflowStandardParams.getWorkflowElement().getArtifactVariables();
    if (isEmpty(artifactVariables)) {
      return map;
    }

    Map<String, Artifact> artifactMapForPhase = getArtifactMapForPhase();
    if (isEmpty(artifactMapForPhase)) {
      return map;
    }

    for (ArtifactVariable artifactVariable : artifactVariables) {
      if (!isArtifactVariableForService(serviceId, artifactVariable)) {
        continue;
      }

      Artifact artifact = artifactMapForPhase.getOrDefault(artifactVariable.getName(), null);
      // TODO: ASR: throw error if null?
      if (artifact != null) {
        map.put(artifactVariable.getName(), artifact);
      }
    }
    return map;
  }

  @Override
  public Artifact getDefaultArtifactForService(String serviceId) {
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, getAccountId())) {
      Map<String, Artifact> map = getArtifactsForService(serviceId);
      return map.getOrDefault(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME, null);
    } else {
      return getArtifactForService(serviceId);
    }
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
  public Application fetchRequiredApp() {
    WorkflowStandardParams stdParam = getContextElement(ContextElementType.STANDARD);
    if (stdParam != null) {
      return stdParam.fetchRequiredApp();
    }
    throw new InvalidRequestException("WorkflowStandardParams cannot be null");
  }

  @Override
  public Environment fetchRequiredEnvironment() {
    WorkflowStandardParams stdParam = getContextElement(ContextElementType.STANDARD);
    if (stdParam != null) {
      return stdParam.fetchRequiredEnv();
    }
    throw new InvalidRequestException("WorkflowStandardParams cannot be null");
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
    return evaluator.substitute(
        expression, context, variableResolverTracker, normalizeStateName(stateExecutionInstance.getDisplayName()));
  }

  private Object normalizeAndEvaluate(
      String expression, Map<String, Object> context, List<String> defaultObjectPrefixes) {
    if (expression == null) {
      return null;
    }
    List<ExpressionProcessor> expressionProcessors = new ArrayList<>();
    Matcher matcher = ManagerExpressionEvaluator.wingsVariablePattern.matcher(expression);

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
        for (String defaultObjectPrefix : defaultObjectPrefixes) {
          String normalizedVariable = defaultObjectPrefix + "." + variable;
          try {
            evaluate(normalizedVariable, new HashMap<>(), context, new ArrayList<>());
          } catch (RuntimeException exception) {
            continue;
          }
          variable = normalizedVariable;
          break;
        }
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

    if (isNotEmpty(normalizedExpressionMap)) {
      logger.info("The above code seems obsolete, but if you see me in the logs, it is not");
    }

    return evaluate(sb.toString(), normalizedExpressionMap, context, defaultObjectPrefixes);
  }

  private Object evaluate(String expr, Map<String, String> normalizedExpressionMap, Map<String, Object> context,
      List<String> defaultObjectPrefixes) {
    Map<String, Object> evaluatedValueMap = new HashMap<>(context);
    for (Entry<String, String> entry : normalizedExpressionMap.entrySet()) {
      String key = entry.getKey();
      Object val = evaluator.evaluate(normalizedExpressionMap.get(key), context);
      if (val instanceof String) {
        String valStr = (String) val;
        Matcher matcher = ManagerExpressionEvaluator.wingsVariablePattern.matcher(valStr);
        if (matcher.find()) {
          val = normalizeAndEvaluate(valStr, context, defaultObjectPrefixes);
        }
      }
      evaluatedValueMap.put(key, val);
    }

    logger.debug("expr: {}, evaluatedValueMap: {}", expr, evaluatedValueMap);
    return evaluator.evaluate(expr, evaluatedValueMap);
  }

  @Override
  public Map<String, Object> asMap() {
    Map<String, Object> context = new LateBindingMap();
    context.putAll(prepareContext(null));
    return context;
  }

  private String normalizeStateName(String name) {
    Matcher matcher = wildCharPattern.matcher(name);
    return matcher.replaceAll("__");
  }

  @Builder
  static class ServiceEncryptedVariable implements LateBindingValue {
    private ServiceVariable serviceVariable;
    private ExecutionContextImpl executionContext;
    private boolean adoptDelegateDecryption;
    private FeatureFlagService featureFlagService;
    private int expressionFunctorToken;

    @Override
    public Object bind() {
      if (adoptDelegateDecryption
          && featureFlagService.isEnabled(FeatureName.THREE_PHASE_SECRET_DECRYPTION, executionContext.getAccountId())) {
        return "${secretManager.obtain(\"" + serviceVariable.getSecretTextName() + "\", " + expressionFunctorToken
            + ")}";
      }
      executionContext.managerDecryptionService.decrypt(serviceVariable,
          executionContext.secretManager.getEncryptionDetails(
              serviceVariable, executionContext.getAppId(), executionContext.getWorkflowExecutionId()));
      final SecretString value = SecretString.builder().value(new String(serviceVariable.getValue())).build();

      // Cache the secret as service variable if they are available.
      if (executionContext.contextMap.containsKey(SERVICE_VARIABLE)) {
        ((Map<String, Object>) executionContext.contextMap.get(SERVICE_VARIABLE)).put(serviceVariable.getName(), value);
      }
      return value;
    }
  }

  private void prepareVariables(EncryptedFieldMode encryptedFieldMode, ServiceVariable serviceVariable,
      Map<String, Object> variables, boolean adoptDelegateDecryption, int expressionFunctorToken) {
    final String variableName = renderExpression(serviceVariable.getName());

    if (!variables.containsKey(variableName) && Type.ARTIFACT != serviceVariable.getType()) {
      if (serviceVariable.getType() == TEXT || encryptedFieldMode == MASKED) {
        variables.put(variableName, renderExpression(new String(serviceVariable.getValue())));
      } else if (Type.ARTIFACT != serviceVariable.getType()) {
        if (isEmpty(serviceVariable.getAccountId())) {
          Application app = getApp();
          notNullCheck("app", app);
          serviceVariable.setAccountId(app.getAccountId());
        }
        variables.put(variableName,
            ServiceEncryptedVariable.builder()
                .serviceVariable(serviceVariable)
                .adoptDelegateDecryption(adoptDelegateDecryption)
                .expressionFunctorToken(expressionFunctorToken)
                .executionContext(this)
                .featureFlagService(featureFlagService)
                .build());
      }
    }
  }

  @Builder
  static class ServiceVariables implements LateBindingValue {
    private EncryptedFieldMode encryptedFieldMode;
    private List<NameValuePair> phaseOverrides;

    private ExecutionContextImpl executionContext;
    private ManagerDecryptionService managerDecryptionService;
    private SecretManager secretManager;
    private boolean adoptDelegateDecryption;
    private int expressionFunctorToken;

    @Override
    public Object bind() {
      String key = encryptedFieldMode == OBTAIN_VALUE ? SERVICE_VARIABLE : SAFE_DISPLAY_SERVICE_VARIABLE;
      executionContext.contextMap.remove(key);

      Map<String, Object> variables = isEmpty(phaseOverrides)
          ? new HashMap<>()
          : phaseOverrides.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

      final List<ServiceVariable> serviceVariables = executionContext.prepareServiceVariables(
          encryptedFieldMode == MASKED ? EncryptedFieldComputeMode.MASKED : EncryptedFieldComputeMode.OBTAIN_META);

      if (isNotEmpty(serviceVariables)) {
        serviceVariables.forEach(serviceVariable -> {
          executionContext.prepareVariables(
              encryptedFieldMode, serviceVariable, variables, adoptDelegateDecryption, expressionFunctorToken);
        });
      }
      executionContext.contextMap.put(key, variables);
      return variables;
    }
  }

  @Builder
  static class EnvironmentVariables implements LateBindingValue {
    private ExecutionContextImpl executionContext;
    private ServiceVariableService serviceVariableService;
    private ManagerDecryptionService managerDecryptionService;
    private SecretManager secretManager;
    private boolean adoptDelegateDecryption;
    private int expressionFunctorToken;

    @Override
    public Object bind() {
      Map<String, Object> variables = new HashMap<>();

      final Environment environment = executionContext.getEnv();

      if (environment == null) {
        executionContext.contextMap.put(ENVIRONMENT_VARIABLE, variables);
        return variables;
      }

      executionContext.contextMap.remove(ENVIRONMENT_VARIABLE);

      final List<ServiceVariable> serviceVariables = serviceVariableService.getServiceVariablesForEntity(
          executionContext.getAppId(), environment.getUuid(), OBTAIN_VALUE);

      executionContext.prepareServiceVariables(EncryptedFieldComputeMode.OBTAIN_META);

      if (isNotEmpty(serviceVariables)) {
        serviceVariables.forEach(serviceVariable -> {
          executionContext.prepareVariables(EncryptedFieldMode.OBTAIN_VALUE, serviceVariable, variables,
              adoptDelegateDecryption, expressionFunctorToken);
        });
      }
      executionContext.contextMap.put(ENVIRONMENT_VARIABLE, variables);
      return variables;
    }
  }

  private Map<String, Object> prepareCacheContext(StateExecutionContext stateExecutionContext) {
    if (contextMap != null) {
      return contextMap;
    }
    contextMap = new LateBindingMap();

    // add state execution data
    stateExecutionInstance.getStateExecutionMap().forEach(
        (key, value) -> contextMap.put(normalizeStateName(key), value));

    // add context params
    Iterator<ContextElement> it = stateExecutionInstance.getContextElements().descendingIterator();
    while (it.hasNext()) {
      ContextElement contextElement = it.next();

      Map<String, Object> map = contextElement.paramMap(this);
      if (map != null) {
        contextMap.putAll(map);
      }
    }

    boolean adoptDelegateDecryption = false;
    if (stateExecutionContext != null) {
      adoptDelegateDecryption = stateExecutionContext.isAdoptDelegateDecryption();
    }
    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PHASE_PARAM);

    if (stateExecutionContext != null && stateExecutionContext.getScriptType() != null) {
      ShellScriptFunctor shellScriptFunctor =
          ShellScriptFunctor.builder().scriptType(stateExecutionContext.getScriptType()).build();
      evaluator.addFunctor("shell", shellScriptFunctor);
    }

    final ServiceVariablesBuilder serviceVariablesBuilder =
        ServiceVariables.builder()
            .phaseOverrides(phaseElement == null ? null : phaseElement.getVariableOverrides())
            .executionContext(this)
            .managerDecryptionService(managerDecryptionService)
            .secretManager(secretManager)
            .adoptDelegateDecryption(adoptDelegateDecryption)
            .expressionFunctorToken(
                stateExecutionContext == null ? 0 : stateExecutionContext.getExpressionFunctorToken());

    if (phaseElement != null) {
      contextMap.put(SERVICE_VARIABLE, serviceVariablesBuilder.encryptedFieldMode(OBTAIN_VALUE).build());
      contextMap.put(SAFE_DISPLAY_SERVICE_VARIABLE, serviceVariablesBuilder.encryptedFieldMode(MASKED).build());
    }

    final EnvironmentVariables environmentVariables =
        EnvironmentVariables.builder()
            .executionContext(this)
            .serviceVariableService(serviceVariableService)
            .managerDecryptionService(managerDecryptionService)
            .secretManager(secretManager)
            .adoptDelegateDecryption(adoptDelegateDecryption)
            .expressionFunctorToken(
                stateExecutionContext == null ? 0 : stateExecutionContext.getExpressionFunctorToken())
            .build();

    contextMap.put(ENVIRONMENT_VARIABLE, environmentVariables);

    String workflowExecutionId = getWorkflowExecutionId();

    contextMap.putAll(variableProcessor.getVariables(stateExecutionInstance.getContextElements(), workflowExecutionId));

    contextMap.put("context",
        SweepingOutputFunctor.builder()
            .sweepingOutputService(sweepingOutputService)
            .sweepingOutputInquiryBuilder(prepareSweepingOutputInquiryBuilder())
            .build());

    contextMap.put("harnessShellUtils", SubstitutionFunctor.builder().build());

    Application app = getApp();
    if (app != null) {
      Environment env = getEnv();
      contextMap.put("secrets",
          SecretFunctor.builder()
              .managerDecryptionService(managerDecryptionService)
              .secretManager(secretManager)
              .accountId(app.getAccountId())
              .appId(app.getUuid())
              .envId(env != null ? env.getUuid() : null)
              .build());
    }
    return contextMap;
  }

  private Map<String, Object> copyIfNeeded(Map<String, Object> map) {
    return contextMap == map ? new HashMap<>(map) : map;
  }

  private Map<String, Object> prepareContext(StateExecutionContext stateExecutionContext) {
    Map<String, Object> map = prepareCacheContext(stateExecutionContext);
    if (stateExecutionContext == null) {
      return map;
    }

    List<ContextElement> contextElements = stateExecutionContext.getContextElements();
    if (contextElements != null) {
      for (ContextElement contextElement : contextElements) {
        map = copyIfNeeded(map);
        map.putAll(contextElement.paramMap(this));
      }
    }
    StateExecutionData stateExecutionData = stateExecutionContext.getStateExecutionData();
    if (stateExecutionData != null) {
      map = copyIfNeeded(map);
      map.put(normalizeStateName(getStateExecutionInstance().getDisplayName()), stateExecutionData);
      if (isNotEmpty(stateExecutionData.getTemplateVariable())) {
        map = copyIfNeeded(map);
        map.putAll(stateExecutionData.getTemplateVariable());
      }
    }
    if (stateExecutionContext.getArtifact() != null) {
      map = copyIfNeeded(map);
      Application app = getApp();
      if (app != null) {
        addArtifactToContext(artifactStreamService, app.getAccountId(), map, stateExecutionContext.getArtifact());
      }
    }
    if (stateExecutionContext.getArtifactFileName() != null) {
      map.put(ExpressionEvaluator.ARTIFACT_FILE_NAME_VARIABLE, stateExecutionContext.getArtifactFileName());
    }
    return map;
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
  public String getPipelineStageElementId() {
    return stateExecutionInstance.getPipelineStageElementId();
  }

  @Override
  public int getPipelineStageParallelIndex() {
    return stateExecutionInstance.getPipelineStageParallelIndex();
  }

  @Override
  public String getPipelineStageName() {
    return stateExecutionInstance.getStageName();
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
  public String getAccountId() {
    final ContextElement contextElement = getContextElement(ContextElementType.STANDARD);
    if (!(contextElement instanceof WorkflowStandardParams)) {
      return null;
    }

    Application app = ((WorkflowStandardParams) contextElement).getApp();
    notNullCheck("app", app);
    return app.getAccountId();
  }

  @Override
  public String getStateExecutionInstanceName() {
    return stateExecutionInstance.getDisplayName();
  }

  @Override
  public Map<String, Object> getServiceVariables() {
    return getServiceVariables(OBTAIN_VALUE);
  }

  @Override
  public Map<String, String> getSafeDisplayServiceVariables() {
    Map<String, Object> map = getServiceVariables(MASKED);
    return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getServiceVariables(EncryptedFieldMode encryptedFieldMode) {
    List<ServiceVariable> serviceVariables = prepareServiceVariables(
        encryptedFieldMode == MASKED ? EncryptedFieldComputeMode.MASKED : EncryptedFieldComputeMode.OBTAIN_VALUE);

    Map<String, Object> variables = new HashMap<>();
    if (isNotEmpty(serviceVariables)) {
      serviceVariables.forEach(serviceVariable -> {
        if (Type.ARTIFACT != serviceVariable.getType()) {
          String name = renderExpression(serviceVariable.getName());
          if (serviceVariable.isDecrypted()) {
            variables.put(name, SecretString.builder().value(new String(serviceVariable.getValue())).build());
          } else {
            variables.put(name, renderExpression(new String(serviceVariable.getValue())));
          }
        }
      });
    }

    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    if (phaseElement != null && isNotEmpty(phaseElement.getVariableOverrides())) {
      variables.putAll(phaseElement.getVariableOverrides().stream().collect(
          Collectors.toMap(NameValuePair::getName, NameValuePair::getValue)));
    }

    if (contextMap != null) {
      String key = encryptedFieldMode == MASKED ? SAFE_DISPLAY_SERVICE_VARIABLE : SERVICE_VARIABLE;
      contextMap.put(key, variables);
    }

    return variables;
  }

  protected List<ServiceVariable> prepareServiceVariables(EncryptedFieldComputeMode encryptedFieldComputeMode) {
    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    if (phaseElement == null || phaseElement.getServiceElement() == null
        || phaseElement.getServiceElement().getUuid() == null) {
      return null;
    }

    Environment env = getEnv();
    notNullCheck("env", env);
    String envId = env.getUuid();
    Optional<Key<ServiceTemplate>> serviceTemplateKey =
        serviceTemplateService
            .getTemplateRefKeysByService(getAppId(), phaseElement.getServiceElement().getUuid(), envId)
            .stream()
            .findFirst();
    if (!serviceTemplateKey.isPresent()) {
      return null;
    }
    return serviceTemplateService.computeServiceVariables(getAppId(), envId, (String) serviceTemplateKey.get().getId(),
        getWorkflowExecutionId(), encryptedFieldComputeMode);
  }

  @Override
  public SettingValue getGlobalSettingValue(String accountId, String settingId) {
    return settingsService.getSettingValueById(accountId, settingId);
  }

  @Override
  public SweepingOutputInstanceBuilder prepareSweepingOutputBuilder(SweepingOutputInstance.Scope sweepingOutputScope) {
    // Default scope is pipeline
    if (sweepingOutputScope == null) {
      sweepingOutputScope = Scope.PIPELINE;
    }
    WorkflowStandardParams workflowStandardParams = getContextElement(ContextElementType.STANDARD);
    String pipelineExecutionId = workflowStandardParams == null || workflowStandardParams.getWorkflowElement() == null
        ? null
        : workflowStandardParams.getWorkflowElement().getPipelineDeploymentUuid();
    String workflowExecutionId = getWorkflowExecutionId();

    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String phaseExecutionId =
        phaseElement == null ? null : workflowExecutionId + phaseElement.getUuid() + phaseElement.getPhaseName();

    String stateExecutionId = stateExecutionInstance.getUuid();

    final SweepingOutputInstanceBuilder sweepingOutputBuilder = SweepingOutputServiceImpl.prepareSweepingOutputBuilder(
        getAppId(), pipelineExecutionId, workflowExecutionId, phaseExecutionId, stateExecutionId, sweepingOutputScope);
    return sweepingOutputBuilder.uuid(generateUuid());
  }

  @Override
  public SweepingOutputInquiryBuilder prepareSweepingOutputInquiryBuilder() {
    SweepingOutputInquiryBuilder builder = SweepingOutputInquiry.builder().appId(getAppId());

    WorkflowStandardParams workflowStandardParams = getContextElement(ContextElementType.STANDARD);
    builder.pipelineExecutionId(workflowStandardParams == null || workflowStandardParams.getWorkflowElement() == null
            ? null
            : workflowStandardParams.getWorkflowElement().getPipelineDeploymentUuid());

    String workflowExecutionId = getWorkflowExecutionId();
    builder.workflowExecutionId(getWorkflowExecutionId());

    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String phaseExecutionId =
        phaseElement == null ? null : workflowExecutionId + phaseElement.getUuid() + phaseElement.getPhaseName();
    builder.phaseExecutionId(phaseExecutionId);

    builder.stateExecutionId(stateExecutionInstance.getUuid());
    return builder;
  }

  public void populateNamespaceInInfraMappingElement(
      InfrastructureMapping infrastructureMapping, InfraMappingElementBuilder builder) {
    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());

    if ((DeploymentType.KUBERNETES == deploymentType) || (DeploymentType.HELM == deploymentType)) {
      String namespace = null;

      if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
        namespace = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
      } else if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
        namespace = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
      } else if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
        namespace = ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
      } else {
        unhandled(infrastructureMapping.getInfraMappingType());
      }

      if (isBlank(namespace)) {
        namespace = "default";
      }

      builder.kubernetes(Kubernetes.builder()
                             .namespace(namespace)
                             .infraId(getNormalizedInfraMappingIdLabelValue(infrastructureMapping.getUuid()))
                             .build());
    }
  }

  public void populateDeploymentSpecificInfoInInfraMappingElement(
      InfrastructureMapping infrastructureMapping, PhaseElement phaseElement, InfraMappingElementBuilder builder) {
    String infraMappingId = fetchInfraMappingId();
    notNullCheck("infraMappingId", infraMappingId);

    if (DeploymentType.PCF.name().equals(phaseElement.getDeploymentType())) {
      PcfInfrastructureMapping pcfInfrastructureMapping = (PcfInfrastructureMapping) infrastructureMapping;

      String route = isNotEmpty(pcfInfrastructureMapping.getRouteMaps())
          ? pcfInfrastructureMapping.getRouteMaps().get(0)
          : StringUtils.EMPTY;
      String tempRoute = isNotEmpty(pcfInfrastructureMapping.getTempRouteMap())
          ? pcfInfrastructureMapping.getTempRouteMap().get(0)
          : StringUtils.EMPTY;

      CloudProvider cloudProvider = generateCloudProviderElement(infrastructureMapping);
      builder.pcf(Pcf.builder()
                      .route(route)
                      .tempRoute(tempRoute)
                      .organization(pcfInfrastructureMapping.getOrganization())
                      .space(pcfInfrastructureMapping.getSpace())
                      .cloudProvider(cloudProvider)
                      .build());
    } else if (DeploymentType.HELM.name().equals(phaseElement.getDeploymentType())) {
      builder.helm(Helm.builder()
                       .shortId(infraMappingId.substring(0, 7).toLowerCase().replace('-', 'z').replace('_', 'z'))
                       .build());
    }
  }

  private CloudProvider generateCloudProviderElement(InfrastructureMapping infrastructureMapping) {
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    if (settingAttribute == null) {
      return null;
    }

    return CloudProvider.builder().name(settingAttribute.getName()).build();
  }

  @Override
  public InfraMappingElement fetchInfraMappingElement() {
    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String infraMappingId = fetchInfraMappingId();
    if (infraMappingId == null) {
      return null;
    }
    String appId = getAppId();
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infrastructureMapping == null) {
      return null;
    }

    String name = infrastructureMapping.getName();
    if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureMapping.getAccountId())) {
      InfrastructureDefinition infrastructureDefinition =
          infrastructureDefinitionService.get(appId, infrastructureMapping.getInfrastructureDefinitionId());
      name = infrastructureDefinition.getName();
    }
    InfraMappingElementBuilder builder =
        InfraMappingElement.builder().name(name).infraId(infrastructureMapping.getUuid());
    populateNamespaceInInfraMappingElement(infrastructureMapping, builder);
    populateDeploymentSpecificInfoInInfraMappingElement(infrastructureMapping, phaseElement, builder);

    return builder.build();
  }

  @Override
  public ServiceElement fetchServiceElement() {
    ServiceElement serviceElement = getContextElement(ContextElementType.SERVICE);
    if (serviceElement != null) {
      return serviceElement;
    }

    ServiceTemplateElement serviceTemplateElement = getContextElement(ContextElementType.SERVICE_TEMPLATE);
    if (serviceTemplateElement != null) {
      return serviceTemplateElement.getServiceElement();
    }

    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    if (phaseElement != null) {
      return phaseElement.getServiceElement();
    }
    return null;
  }

  @Override
  public AutoLogContext autoLogContext() {
    final ImmutableMap.Builder<String, String> context = ImmutableMap.builder();
    if (getAccountId() != null) {
      context.put(AccountLogContext.ID, getAccountId());
    }
    if (getAppId() != null) {
      context.put(AppLogContext.ID, getAppId());
    }
    if (getWorkflowId() != null) {
      context.put(WorkflowLogContext.ID, getWorkflowId());
    }
    if (getWorkflowExecutionId() != null) {
      if (getWorkflowType() == PIPELINE) {
        context.put(PipelineWorkflowExecutionLogContext.ID, getWorkflowExecutionId());
      } else {
        context.put(WorkflowExecutionLogContext.ID, getWorkflowExecutionId());
      }
    }
    if (getStateExecutionInstanceId() != null) {
      context.put(StateExecutionInstanceLogContext.ID, getStateExecutionInstanceId());
    }

    return new AutoLogContext(context.build(), OVERRIDE_ERROR);
  }

  @Override
  public String fetchInfraMappingId() {
    PhaseElement phaseElement = getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);

    if (phaseElement == null) {
      return null;
    }
    SweepingOutputInquiry sweepingOutputInquiry =
        prepareSweepingOutputInquiryBuilder()
            .name(InfrastructureConstants.PHASE_INFRA_MAPPING_KEY_NAME + phaseElement.getUuid())
            .build();
    SweepingOutputInstance sweepingOutputInstance = sweepingOutputService.find(sweepingOutputInquiry);
    return sweepingOutputInstance == null
        ? null
        : ((InfraMappingSweepingOutput) sweepingOutputInstance.getValue()).getInfraMappingId();
  }
}

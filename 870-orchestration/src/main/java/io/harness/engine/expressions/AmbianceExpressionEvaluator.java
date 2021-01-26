package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.core.Recaster;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.expressions.functors.ExecutionSweepingOutputFunctor;
import io.harness.engine.expressions.functors.NodeExecutionAncestorFunctor;
import io.harness.engine.expressions.functors.NodeExecutionChildFunctor;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.engine.expressions.functors.NodeExecutionQualifiedFunctor;
import io.harness.engine.expressions.functors.OutcomeFunctor;
import io.harness.engine.expressions.functors.SecretFunctor;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.EngineJexlContext;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.JsonFunctor;
import io.harness.expression.RegexFunctor;
import io.harness.expression.ResolveObjectResponse;
import io.harness.expression.VariableResolverTracker;
import io.harness.expression.XmlFunctor;
import io.harness.expression.field.dummy.DummyOrchestrationField;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.OrchestrationField;
import io.harness.pms.expression.OrchestrationFieldProcessor;
import io.harness.pms.expression.OrchestrationFieldType;
import io.harness.pms.expression.ProcessorResult;
import io.harness.pms.sdk.core.registries.OrchestrationFieldRegistry;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import org.bson.Document;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * AmbianceExpressionEvaluator is the basic expression evaluator provided by the orchestration engine. It provides
 * support for expressions based on the runtime graph, outcomes and sweeping output. It contains other helpful
 * functors like regex, json and xml. Apart from this, it also supports static and group based aliases. All these
 * concepts are explained in detail here:
 * https://harness.atlassian.net/wiki/spaces/WR/pages/722536048/Expression+Evaluation.
 *
 * In order to add support for custom expressions/functors, users need to extend this class and override 2 methods -
 * {@link #initialize()} and {@link #fetchPrefixes()}. This subclass needs a corresponding {@link
 * ExpressionEvaluatorProvider} to be provided when adding a dependency on {@link io.harness.OrchestrationModule}. For a
 * sample implementation, look at SampleExpressionEvaluator.java and SampleExpressionEvaluatorProvider.java.
 */
@OwnedBy(CDC)
public class AmbianceExpressionEvaluator extends EngineExpressionEvaluator {
  @Inject private PmsOutcomeService pmsOutcomeService;
  @Inject private PmsSweepingOutputService pmsSweepingOutputService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationFieldRegistry orchestrationFieldRegistry;

  protected final Ambiance ambiance;
  private final Set<NodeExecutionEntityType> entityTypes;
  private final boolean refObjectSpecific;
  private final Map<String, String> groupAliases;

  @Builder
  public AmbianceExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    super(variableResolverTracker);
    this.ambiance = ambiance;
    //    if (ambiance.getExpressionFunctorToken() == 0) {
    //      ambiance.setExpressionFunctorToken(HashGenerator.generateIntegerHash());
    //    }

    this.entityTypes = entityTypes == null ? NodeExecutionEntityType.allEntities() : entityTypes;
    this.refObjectSpecific = refObjectSpecific;
    this.groupAliases = new HashMap<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initialize() {
    super.initialize();
    if (!refObjectSpecific) {
      // Add basic functors.
      addToContext("regex", new RegexFunctor());
      addToContext("json", new JsonFunctor());
      addToContext("xml", new XmlFunctor());
      addToContext("secrets",
          new SecretFunctor(Integer.parseInt(ambiance.getSetupAbstractionsMap().get("expressionFunctorToken"))));
    }

    if (entityTypes.contains(NodeExecutionEntityType.OUTCOME)) {
      addToContext("outcome", OutcomeFunctor.builder().ambiance(ambiance).pmsOutcomeService(pmsOutcomeService).build());
    }

    if (entityTypes.contains(NodeExecutionEntityType.SWEEPING_OUTPUT)) {
      addToContext("output",
          ExecutionSweepingOutputFunctor.builder()
              .pmsSweepingOutputService(pmsSweepingOutputService)
              .ambiance(ambiance)
              .build());
    }

    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    if (planExecution == null) {
      return;
    }

    NodeExecutionsCache nodeExecutionsCache = new NodeExecutionsCache(nodeExecutionService, ambiance);
    // Access StepParameters and Outcomes of self and children.
    addToContext("child",
        NodeExecutionChildFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .pmsOutcomeService(pmsOutcomeService)
            .pmsSweepingOutputService(pmsSweepingOutputService)
            .ambiance(ambiance)
            .entityTypes(entityTypes)
            .build());
    // Access StepParameters and Outcomes of ancestors.
    addToContext("ancestor",
        NodeExecutionAncestorFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .pmsOutcomeService(pmsOutcomeService)
            .pmsSweepingOutputService(pmsSweepingOutputService)
            .ambiance(ambiance)
            .entityTypes(entityTypes)
            .groupAliases(groupAliases)
            .build());
    // Access StepParameters and Outcomes using fully qualified names.
    addToContext("qualified",
        NodeExecutionQualifiedFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .pmsOutcomeService(pmsOutcomeService)
            .pmsSweepingOutputService(pmsSweepingOutputService)
            .ambiance(ambiance)
            .entityTypes(entityTypes)
            .build());
  }

  /**
   * Add a group alias. Any expression that starts with `aliasName` will be replaced by the identifier of the first
   * ancestor node with the given groupName. Should be called within the initialize method only.
   *
   * @param aliasName   the name of the alias
   * @param groupName the name of the group
   */
  protected void addGroupAlias(@NotNull String aliasName, @NotNull String groupName) {
    if (isInitialized()) {
      return;
    }
    if (!validAliasName(aliasName)) {
      throw new InvalidRequestException("Invalid alias: " + aliasName);
    }
    groupAliases.put(aliasName, groupName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @NotEmpty
  protected List<String> fetchPrefixes() {
    ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
    if (entityTypes.contains(NodeExecutionEntityType.OUTCOME)) {
      listBuilder.add("outcome");
    }
    if (entityTypes.contains(NodeExecutionEntityType.SWEEPING_OUTPUT)) {
      listBuilder.add("output");
    }
    return listBuilder.add("child").add("ancestor").add("qualified").addAll(super.fetchPrefixes()).build();
  }

  @Override
  public Object resolve(Object o) {
    return ExpressionEvaluatorUtils.updateExpressions(
        o, new AmbianceResolveFunctorImpl(this, orchestrationFieldRegistry, ambiance));
  }

  @Override
  protected Object evaluateInternal(String expression, EngineJexlContext ctx) {
    Object value = super.evaluateInternal(expression, ctx);
    if (value instanceof OrchestrationField) {
      OrchestrationField orchestrationField = (OrchestrationField) value;
      return orchestrationField.fetchFinalValue();
    } else if (!(value instanceof Document)) {
      return value;
    }

    Document doc = (Document) value;
    String recastedClass = (String) doc.getOrDefault(Recaster.RECAST_CLASS_KEY, "");
    if (recastedClass.equals(ParameterField.class.getName())
        || recastedClass.equals(DummyOrchestrationField.class.getName())) {
      OrchestrationField orchestrationField = RecastOrchestrationUtils.fromDocument(doc, OrchestrationField.class);
      return orchestrationField.fetchFinalValue();
    }
    return value;
  }

  public static class AmbianceResolveFunctorImpl extends ResolveFunctorImpl {
    private final OrchestrationFieldRegistry orchestrationFieldRegistry;
    private final Ambiance ambiance;

    public AmbianceResolveFunctorImpl(AmbianceExpressionEvaluator expressionEvaluator,
        OrchestrationFieldRegistry orchestrationFieldRegistry, Ambiance ambiance) {
      super(expressionEvaluator);
      this.orchestrationFieldRegistry = orchestrationFieldRegistry;
      this.ambiance = ambiance;
    }

    /**
     * <p>
     * This method checks if we have special objects, applies custom handling for them and mutates these objects
     * <br>
     * Special objects: {@link Document} and {@link OrchestrationField}.
     * </p>
     * <p>
     * When we encounter a {@link Document} with {@link Recaster#RECAST_CLASS_KEY} specified
     * and equal to one of these values:
     * <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;{@link ParameterField},
     * <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;{@link DummyOrchestrationField} - used only for testing
     * <br>
     * We recast it from <code>Document</code> to <code>OrchestrationField</code> and send to
     * <br>
     * <code>getProcessorResult(OrchestrationField orchestrationField)</code> method
     * <br>
     * which mutates this object.
     * <br>
     * Then we recast this updated <code>OrchestrationField</code> back to Document and update (replace) original
     * <code>Object o</code> value
     * </p>
     *
     * <p>
     * When we encounter {@link OrchestrationField} we simply call this method
     * <code>getProcessorResult(OrchestrationField orchestrationField)</code> and update the object
     * </p>
     *
     * @param o an object that should be processed
     * @return {@link ResolveObjectResponse} with 2 boolean flags: processed and changed
     */
    @Override
    public ResolveObjectResponse processObject(Object o) {
      OrchestrationField orchestrationField;
      if (o instanceof Document) {
        Document doc = (Document) o;
        String recastedClass = (String) doc.getOrDefault(Recaster.RECAST_CLASS_KEY, "");
        if (recastedClass.equals(ParameterField.class.getName())
            || recastedClass.equals(DummyOrchestrationField.class.getName())) {
          orchestrationField = RecastOrchestrationUtils.fromDocument(doc, OrchestrationField.class);
          ProcessorResult processorResult = getProcessorResult(orchestrationField);
          Document recastedDocument = RecastOrchestrationUtils.toDocument(orchestrationField);
          doc.clear();
          doc.putAll(recastedDocument);
          return new ResolveObjectResponse(true, processorResult.getStatus() == ProcessorResult.Status.CHANGED);
        } else {
          return new ResolveObjectResponse(false, false);
        }
      } else if (o instanceof OrchestrationField) {
        orchestrationField = (OrchestrationField) o;
        ProcessorResult processorResult = getProcessorResult(orchestrationField);
        return new ResolveObjectResponse(true, processorResult.getStatus() == ProcessorResult.Status.CHANGED);
      } else {
        return new ResolveObjectResponse(false, false);
      }
    }

    private ProcessorResult getProcessorResult(OrchestrationField orchestrationField) {
      OrchestrationFieldType type = orchestrationField.getType();
      OrchestrationFieldProcessor fieldProcessor = orchestrationFieldRegistry.obtain(type);
      ProcessorResult processorResult = fieldProcessor.process(ambiance, orchestrationField);
      if (processorResult.getStatus() == ProcessorResult.Status.ERROR) {
        throw new CriticalExpressionEvaluationException(processorResult.getMessage());
      }
      return processorResult;
    }
  }
}

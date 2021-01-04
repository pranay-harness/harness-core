package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.expressions.functors.*;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.expression.*;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.OrchestrationField;
import io.harness.pms.expression.OrchestrationFieldProcessor;
import io.harness.pms.expression.OrchestrationFieldType;
import io.harness.pms.expression.ProcessorResult;
import io.harness.pms.sdk.core.registries.OrchestrationFieldRegistry;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
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

    @Override
    public ResolveObjectResponse processObject(Object o) {
      if (!(o instanceof OrchestrationField)) {
        return new ResolveObjectResponse(false, false);
      }

      OrchestrationField orchestrationField = (OrchestrationField) o;
      OrchestrationFieldType type = orchestrationField.getType();
      OrchestrationFieldProcessor fieldProcessor = orchestrationFieldRegistry.obtain(type);
      ProcessorResult processorResult = fieldProcessor.process(ambiance, orchestrationField);
      if (processorResult.getStatus() == ProcessorResult.Status.ERROR) {
        throw new CriticalExpressionEvaluationException(processorResult.getMessage());
      }

      return new ResolveObjectResponse(true, processorResult.getStatus() == ProcessorResult.Status.CHANGED);
    }
  }
}

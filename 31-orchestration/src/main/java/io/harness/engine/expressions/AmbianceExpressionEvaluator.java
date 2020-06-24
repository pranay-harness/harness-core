package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.algorithm.HashGenerator;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.expressions.functors.ExecutionSweepingOutputFunctor;
import io.harness.engine.expressions.functors.NodeExecutionAncestorFunctor;
import io.harness.engine.expressions.functors.NodeExecutionChildFunctor;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.engine.expressions.functors.NodeExecutionQualifiedFunctor;
import io.harness.engine.expressions.functors.OutcomeFunctor;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.PlanExecution;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@EqualsAndHashCode(callSuper = true)
public class AmbianceExpressionEvaluator extends EngineExpressionEvaluator {
  @Inject private OutcomeService outcomeService;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  private final Ambiance ambiance;
  private final Set<NodeExecutionEntityType> entityTypes;
  private final boolean refObjectSpecific;

  @Builder
  public AmbianceExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    super(variableResolverTracker);
    this.ambiance = ambiance;
    if (ambiance.getExpressionFunctorToken() == 0) {
      ambiance.setExpressionFunctorToken(HashGenerator.generateIntegerHash());
    }

    this.entityTypes = entityTypes == null ? NodeExecutionEntityType.allEntities() : entityTypes;
    this.refObjectSpecific = refObjectSpecific;
  }

  @Override
  protected void initialize() {
    if (!refObjectSpecific) {
      super.initialize();
    }

    if (entityTypes.contains(NodeExecutionEntityType.OUTCOME)) {
      addToContext("outcome", OutcomeFunctor.builder().ambiance(ambiance).outcomeService(outcomeService).build());
    }

    if (entityTypes.contains(NodeExecutionEntityType.SWEEPING_OUTPUT)) {
      addToContext("output",
          ExecutionSweepingOutputFunctor.builder()
              .executionSweepingOutputService(executionSweepingOutputService)
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
            .outcomeService(outcomeService)
            .executionSweepingOutputService(executionSweepingOutputService)
            .ambiance(ambiance)
            .entityTypes(entityTypes)
            .build());
    // Access StepParameters and Outcomes of ancestors.
    addToContext("ancestor",
        NodeExecutionAncestorFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .outcomeService(outcomeService)
            .executionSweepingOutputService(executionSweepingOutputService)
            .ambiance(ambiance)
            .entityTypes(entityTypes)
            .build());
    // Access StepParameters and Outcomes using fully qualified names.
    addToContext("qualified",
        NodeExecutionQualifiedFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .outcomeService(outcomeService)
            .executionSweepingOutputService(executionSweepingOutputService)
            .ambiance(ambiance)
            .entityTypes(entityTypes)
            .build());
  }

  @Override
  @NotNull
  protected List<String> fetchPrefixes() {
    ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
    if (entityTypes.contains(NodeExecutionEntityType.OUTCOME)) {
      listBuilder.add("outcome");
    }
    if (entityTypes.contains(NodeExecutionEntityType.SWEEPING_OUTPUT)) {
      listBuilder.add("output");
    }

    return listBuilder.add("child").add("ancestor").add("qualified").add("").build();
  }
}

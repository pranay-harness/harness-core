package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.expressions.functors.ExecutionSweepingOutputFunctor;
import io.harness.engine.expressions.functors.NodeExecutionAncestorFunctor;
import io.harness.engine.expressions.functors.NodeExecutionChildFunctor;
import io.harness.engine.expressions.functors.NodeExecutionQualifiedFunctor;
import io.harness.engine.expressions.functors.OutcomeFunctor;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.OutcomeService;
import io.harness.execution.PlanExecution;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputResolver;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@ExcludeRedesign
@EqualsAndHashCode(callSuper = true)
public class EngineAmbianceExpressionEvaluator extends EngineExpressionEvaluator {
  @Inject private OutcomeService outcomeService;
  @Inject private ExecutionSweepingOutputResolver executionSweepingOutputResolver;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private NodeExecutionService nodeExecutionService;

  private final Ambiance ambiance;

  @Builder
  public EngineAmbianceExpressionEvaluator(Ambiance ambiance, VariableResolverTracker variableResolverTracker) {
    super(variableResolverTracker);
    this.ambiance = ambiance;
  }

  @Override
  protected void initialize() {
    super.initialize();
    addToContext("outcome", OutcomeFunctor.builder().ambiance(ambiance).outcomeService(outcomeService).build());
    addToContext("output",
        ExecutionSweepingOutputFunctor.builder()
            .executionSweepingOutputResolver(executionSweepingOutputResolver)
            .ambiance(ambiance)
            .build());

    PlanExecution planExecution = ambianceHelper.obtainExecutionInstance(ambiance);
    if (planExecution == null) {
      return;
    }

    NodeExecutionsCache nodeExecutionsCache = new NodeExecutionsCache(nodeExecutionService, ambiance);
    // Access StepParameters and Outcomes of self and children.
    addToContext("child",
        NodeExecutionChildFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .outcomeService(outcomeService)
            .ambiance(ambiance)
            .build());
    // Access StepParameters and Outcomes of ancestors.
    addToContext("ancestor",
        NodeExecutionAncestorFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .outcomeService(outcomeService)
            .ambiance(ambiance)
            .build());
    // Access StepParameters and Outcomes using fully qualified names.
    addToContext("qualified",
        NodeExecutionQualifiedFunctor.builder()
            .nodeExecutionsCache(nodeExecutionsCache)
            .outcomeService(outcomeService)
            .ambiance(ambiance)
            .build());
  }

  @Override
  @NotNull
  protected List<String> fetchPrefixes() {
    return ImmutableList.of("outcome", "output", "child", "ancestor", "qualified", "");
  }
}

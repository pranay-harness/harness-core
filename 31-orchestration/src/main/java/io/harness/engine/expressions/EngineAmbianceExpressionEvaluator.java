package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.functors.OutcomeFunctor;
import io.harness.engine.services.OutcomeService;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
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

  private final Ambiance ambiance;

  @Builder
  public EngineAmbianceExpressionEvaluator(Ambiance ambiance, VariableResolverTracker variableResolverTracker) {
    super(variableResolverTracker);
    this.ambiance = ambiance;
  }

  @Override
  protected void initialize() {
    super.initialize();
    addToContext("context", OutcomeFunctor.builder().ambiance(ambiance).outcomeService(outcomeService).build());
  }

  @Override
  @NotNull
  protected List<String> fetchPrefixes() {
    return ImmutableList.of("context");
  }
}

package io.harness.ngtriggers.expressions;

import io.harness.expression.EngineExpressionEvaluator;
import io.harness.ngpipeline.expressions.functors.EventPayloadFunctor;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.plan.execution.SetupAbstractionKeys;

public class TriggerExpressionEvaluator extends EngineExpressionEvaluator {
  Ambiance ambiance;
  public TriggerExpressionEvaluator(String payload) {
    super(null);
    this.ambiance = Ambiance.newBuilder()
                        .setMetadata(ExecutionMetadata.newBuilder()
                                         .setTriggerPayload(TriggerPayload.newBuilder().setJsonPayload(payload).build())
                                         .build())
                        .build();
  }
  @Override
  protected void initialize() {
    addToContext(SetupAbstractionKeys.eventPayload, new EventPayloadFunctor(ambiance));
  }
}

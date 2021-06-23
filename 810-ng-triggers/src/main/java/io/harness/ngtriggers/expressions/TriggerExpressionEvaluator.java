package io.harness.ngtriggers.expressions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.ngtriggers.expressions.functors.PayloadFunctor;
import io.harness.ngtriggers.expressions.functors.TriggerPayloadFunctor;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerExpressionEvaluator extends EngineExpressionEvaluator {
  private final String payload;
  private final TriggerPayload triggerPayload;

  public TriggerExpressionEvaluator(
      ParseWebhookResponse parseWebhookResponse, List<HeaderConfig> headerConfigs, String payload) {
    super(null);
    TriggerPayload.Builder builder = TriggerPayload.newBuilder();
    if (parseWebhookResponse != null) {
      if (parseWebhookResponse.hasPr()) {
        builder.setParsedPayload(ParsedPayload.newBuilder().setPr(parseWebhookResponse.getPr()).build()).build();
      } else {
        builder.setParsedPayload(ParsedPayload.newBuilder().setPush(parseWebhookResponse.getPush()).build()).build();
      }
    }
    if (headerConfigs != null) {
      for (HeaderConfig config : headerConfigs) {
        if (config != null) {
          builder.putHeaders(config.getKey().toLowerCase(), config.getValues().get(0));
        }
      }
    }
    this.triggerPayload = builder.build();
    Ambiance.newBuilder().setMetadata(ExecutionMetadata.newBuilder().build()).build();
    this.payload = payload;
  }
  @Override
  protected void initialize() {
    addToContext(SetupAbstractionKeys.trigger, new TriggerPayloadFunctor(payload, triggerPayload));
    addToContext(SetupAbstractionKeys.eventPayload, new PayloadFunctor(payload));
  }
}

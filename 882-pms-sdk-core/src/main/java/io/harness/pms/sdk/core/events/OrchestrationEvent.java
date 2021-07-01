package io.harness.pms.sdk.core.events;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "OrchestrationEventKeys")
public class OrchestrationEvent {
  @NotNull Ambiance ambiance;
  String serviceName;
  Status status;
  StepParameters resolvedStepParameters;

  @NotNull OrchestrationEventType eventType;
  TriggerPayload triggerPayload;
  List<String> tags;

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = AmbianceUtils.logContextMap(ambiance);
    logContext.put(OrchestrationEventKeys.eventType, eventType.name());
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }
}

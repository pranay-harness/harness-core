package io.harness.pms.sdk.core.events;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.queue.Queuable;
import io.harness.queue.WithMonitoring;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "OrchestrationEventKeys")
@Entity(value = "orchestrationEventQueue", noClassnameStored = true)
@Document("orchestrationEventQueue")
@HarnessEntity(exportable = false)
@TypeAlias("orchestrationEvent")
public class OrchestrationEvent extends Queuable implements WithMonitoring {
  @NotNull Ambiance ambiance;
  String serviceName;
  Status status;
  String resolvedStepParameters;

  @NotNull OrchestrationEventType eventType;
  @Getter @Setter @NonFinal @CreatedDate Long createdAt;
  @Setter @NonFinal @Version Long version;

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = AmbianceUtils.logContextMap(ambiance);
    logContext.put(OrchestrationEventKeys.eventType, eventType.name());
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }

  @Override
  public ThreadAutoLogContext metricContext() {
    Map<String, String> logContext = new HashMap<>();
    logContext.putAll(AmbianceUtils.logContextMap(ambiance));
    logContext.put("eventType", eventType.name());
    logContext.put("module", getTopic());
    logContext.put("pipelineIdentifier", ambiance.getMetadata().getPipelineIdentifier());
    return new ThreadAutoLogContext(logContext, OVERRIDE_NESTS);
  }

  @Override
  public String getMetricPrefix() {
    return "orchestration_event";
  }
}

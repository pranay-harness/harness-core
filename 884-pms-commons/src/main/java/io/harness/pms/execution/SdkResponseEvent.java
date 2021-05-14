package io.harness.pms.execution;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.queue.Queuable;
import io.harness.queue.WithMonitoring;

import java.util.HashMap;
import java.util.Map;
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

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "SdkResponseEventKeys")
@Entity(value = "SdkResponseEvent", noClassnameStored = true)
@Document("SdkResponseEvent")
@HarnessEntity(exportable = false)
@TypeAlias("SdkResponseEvent")
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEvent extends Queuable implements WithMonitoring {
  SdkResponseEventType sdkResponseEventType;
  SdkResponseEventRequest sdkResponseEventRequest;
  @Getter @Setter @NonFinal @CreatedDate long createdAt;
  @Setter @NonFinal @Version Long version;

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = new HashMap<>();
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }

  @Override
  public ThreadAutoLogContext metricContext() {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("eventType", sdkResponseEventType.name());
    logContext.put("runtimeId", sdkResponseEventRequest.getNodeExecutionId());
    return new ThreadAutoLogContext(logContext, OVERRIDE_NESTS);
  }

  @Override
  public String getMetricPrefix() {
    return "sdk_response_event";
  }
}

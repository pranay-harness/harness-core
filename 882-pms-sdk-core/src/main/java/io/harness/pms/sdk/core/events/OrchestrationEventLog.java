package io.harness.pms.sdk.core.events;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "OrchestrationEventLogKeys")
@Entity(value = "orchestrationEventLog")
@Document("orchestrationEventLog")
@HarnessEntity(exportable = false)
@TypeAlias("OrchestrationEventLog")
public class OrchestrationEventLog {
  @Wither @Id @org.mongodb.morphia.annotations.Id String id;
  String planExecutionId;
  OrchestrationEvent event;
  long createdAt;
}

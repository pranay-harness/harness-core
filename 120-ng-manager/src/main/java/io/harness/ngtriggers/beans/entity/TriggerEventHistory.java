package io.harness.ngtriggers.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.ngtriggers.beans.webhookresponse.WebhookEventResponse;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TriggerEventHistoryKeys")
@Entity(value = "triggerEventHistory", noClassnameStored = true)
@Document("triggerEventHistory")
@TypeAlias("triggerEventHistory")
@HarnessEntity(exportable = true)
public class TriggerEventHistory implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String accountId;
  String eventCorrelationId;
  String payload;
  Long eventCreatedAt;
  WebhookEventResponse.FinalStatus finalStatus;
  String message;
  String planExecutionId;
  boolean exceptionOccurred;
  String triggerIdentifier;

  @CreatedDate Long createdAt;
}

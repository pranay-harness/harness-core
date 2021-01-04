package io.harness.ngtriggers.beans.entity;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TriggerWebhookEventsKeys")
@Entity(value = "triggerWebhookEvents", noClassnameStored = true)
@Document("triggerWebhookEvents")
@TypeAlias("triggerWebhookEvents")
@HarnessEntity(exportable = true)
public class TriggerWebhookEvent implements PersistentEntity, UuidAccess, PersistentRegularIterable {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String payload;
  List<HeaderConfig> headers;
  String accountId;
  @Builder.Default boolean processing = Boolean.FALSE;

  @Builder.Default Integer attemptCount = 0;

  @FdTtlIndex @Default private Date validUntil = Date.from(OffsetDateTime.now().plusDays(7).toInstant());
  @CreatedDate Long createdAt;
  @Builder.Default private Long nextIteration = 0L;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }
}

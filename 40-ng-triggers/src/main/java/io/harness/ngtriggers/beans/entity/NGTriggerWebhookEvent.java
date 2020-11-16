package io.harness.ngtriggers.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.pms.execution.Status;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static java.time.Duration.ofDays;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NGTriggerWebhookEventKeys")
@Entity(value = "triggersNGWebhookEventQueue", noClassnameStored = true)
@Document("triggersNGWebhookEventQueue")
@TypeAlias("triggersNGWebhookEventQueue")
@HarnessEntity(exportable = true)
public class NGTriggerWebhookEvent implements PersistentEntity, UuidAccess {
  public static final Duration TTL = ofDays(7);

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String payload;
  List<HeaderConfig> headers;
  String accountId;

  @Builder.Default Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());

  Status status;
  Long startTs;
  Long retryCount;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  @Builder.Default Boolean deleted = Boolean.FALSE;
}
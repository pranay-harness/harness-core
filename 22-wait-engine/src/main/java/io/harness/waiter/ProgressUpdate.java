package io.harness.waiter;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Represents response generated by a correlationId.
 */
@Value
@Builder
@FieldNameConstants(innerTypeName = "ProgressUpdateKeys")
@Entity(value = "progressUpdate", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ProgressUpdate implements PersistentEntity, UuidAccess, CreatedAtAccess {
  public static final Duration TTL = ofDays(21);

  @Id private String uuid;
  private String correlationId;
  private long createdAt;
  private byte[] progressData;
  private long expireProcessing;

  @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plus(TTL).toInstant());
}

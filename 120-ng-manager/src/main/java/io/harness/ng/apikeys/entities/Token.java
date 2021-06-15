package io.harness.ng.apikeys.entities;

import io.harness.beans.Scope;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "TokenKeys")
@Entity(value = "tokens", noClassnameStored = true)
@Document("tokens")
@TypeAlias("tokens")
public class Token implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @FdIndex String accountId;
  long createdAt;
  long lastUpdatedAt;

  @FdIndex String apiKeyIdentifier;
  String hash;
  Instant validFrom;
  Instant validTo;
  @Nullable Instant scheduledExpireTime;
  Scope scope;

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());

  @JsonIgnore
  public boolean isValidNow() {
    Instant currentTime = Instant.now();
    // If scheduled Expire time is present, it takes precedence over valid To
    return currentTime.isAfter(validFrom)
        && (scheduledExpireTime != null ? currentTime.isBefore(scheduledExpireTime) : currentTime.isBefore(validTo));
  }
}

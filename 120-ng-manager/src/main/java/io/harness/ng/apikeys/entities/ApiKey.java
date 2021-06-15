package io.harness.ng.apikeys.entities;

import io.harness.beans.Scope;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.apikeys.beans.ApiKeyType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import java.time.Duration;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ApiKeyKeys")
@Entity(value = "apiKeys", noClassnameStored = true)
@Document("apiKeys")
@TypeAlias("apiKeys")
public abstract class ApiKey implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @NotNull @FdIndex String accountId;
  long createdAt;
  long lastUpdatedAt;

  @FdIndex String name;
  @Default long defaultTokenTtlInMillis = Duration.ofDays(31).toMillis();
  Scope scope;

  public abstract ApiKeyType getType();
}

package io.harness.ng.core.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.NGOrgAccess;
import io.harness.ng.core.NGProjectAccess;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TokenKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "tokens", noClassnameStored = true)
@Document("tokens")
@TypeAlias("tokens")
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.PL)
public class Token implements PersistentEntity, UuidAware, NGAccountAccess, NGOrgAccess, NGProjectAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("list_tokens_idx")
                 .field(TokenKeys.accountIdentifier)
                 .field(TokenKeys.orgIdentifier)
                 .field(TokenKeys.projectIdentifier)
                 .field(TokenKeys.apiKeyType)
                 .field(TokenKeys.parentIdentifier)
                 .field(TokenKeys.apiKeyIdentifier)
                 .build())
        .build();
  }

  @org.springframework.data.annotation.Id @Id String uuid;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;

  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String parentIdentifier;
  ApiKeyType apiKeyType;
  String apiKeyIdentifier;

  @FdIndex String identifier;
  String name;
  Instant validFrom;
  Instant validTo;
  Instant scheduledExpireTime;

  @FdTtlIndex private Date validUntil;

  @JsonIgnore
  public Instant getExpiryTimestamp() {
    return scheduledExpireTime != null ? scheduledExpireTime : validTo;
  }

  @JsonIgnore
  public boolean isValid() {
    Instant currentTime = Instant.now();
    return currentTime.isAfter(validFrom) && currentTime.isBefore(getExpiryTimestamp());
  }
}

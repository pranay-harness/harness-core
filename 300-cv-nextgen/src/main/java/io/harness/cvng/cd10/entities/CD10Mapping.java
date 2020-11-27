package io.harness.cvng.cd10.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.cd10.beans.MappingType;
import io.harness.cvng.cd10.entities.CD10EnvMapping.CD10EnvMappingKeys;
import io.harness.cvng.cd10.entities.CD10ServiceMapping.CD10ServiceMappingKeys;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "CD10MappingKeys")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "cd10Mappings")
@HarnessEntity(exportable = true)
public abstract class CD10Mapping
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("env_unique_index")
                 .unique(true)
                 .field(CD10MappingKeys.accountId)
                 .field(CD10MappingKeys.appId)
                 .field(CD10EnvMappingKeys.envId)
                 .build(),
            CompoundMongoIndex.builder()
                .name("service_unique_index")
                .unique(true)
                .field(CD10MappingKeys.accountId)
                .field(CD10MappingKeys.appId)
                .field(CD10ServiceMappingKeys.serviceId)
                .build())
        .build();
  }
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private String accountId;
  private String appId;
  private String orgIdentifier;
  private String projectIdentifier;
  private MappingType type;
}

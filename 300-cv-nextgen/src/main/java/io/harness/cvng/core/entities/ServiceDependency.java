package io.harness.cvng.core.entities;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.mongo.CollationLocale;
import io.harness.mongo.CollationStrength;
import io.harness.mongo.index.Collation;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ServiceDependencyKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "serviceDependencies")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.CVNG)
@OwnedBy(CV)
public class ServiceDependency implements PersistentEntity, UuidAware, AccountAccess, UpdatedAtAware, CreatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_edge_idx")
                 .unique(true)
                 .field(ServiceDependencyKeys.accountId)
                 .field(ServiceDependencyKeys.orgIdentifier)
                 .field(ServiceDependencyKeys.projectIdentifier)
                 .field(ServiceDependencyKeys.environmentIdentifier)
                 .field(ServiceDependencyKeys.fromServiceIdentifier)
                 .field(ServiceDependencyKeys.toServiceIdentifier)
                 .collation(
                     Collation.builder().locale(CollationLocale.ENGLISH).strength(CollationStrength.PRIMARY).build())
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("list_all_edges_idx")
                 .unique(false)
                 .field(ServiceDependencyKeys.accountId)
                 .field(ServiceDependencyKeys.orgIdentifier)
                 .field(ServiceDependencyKeys.projectIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("list_edges_by_env_idx")
                 .unique(false)
                 .field(ServiceDependencyKeys.accountId)
                 .field(ServiceDependencyKeys.orgIdentifier)
                 .field(ServiceDependencyKeys.projectIdentifier)
                 .field(ServiceDependencyKeys.environmentIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("list_dependent_svc_idx")
                 .unique(false)
                 .field(ServiceDependencyKeys.accountId)
                 .field(ServiceDependencyKeys.orgIdentifier)
                 .field(ServiceDependencyKeys.projectIdentifier)
                 .field(ServiceDependencyKeys.environmentIdentifier)
                 .field(ServiceDependencyKeys.toServiceIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("list_dependencies_idx")
                 .unique(false)
                 .field(ServiceDependencyKeys.accountId)
                 .field(ServiceDependencyKeys.orgIdentifier)
                 .field(ServiceDependencyKeys.projectIdentifier)
                 .field(ServiceDependencyKeys.environmentIdentifier)
                 .field(ServiceDependencyKeys.fromServiceIdentifier)
                 .build())
        .build();
  }

  @Id private String uuid;
  @NotNull String accountId;
  @EntityIdentifier String orgIdentifier;
  @EntityIdentifier String projectIdentifier;
  @EntityIdentifier String environmentIdentifier;
  @EntityIdentifier String fromServiceIdentifier;
  @EntityIdentifier String toServiceIdentifier;

  private long lastUpdatedAt;
  private long createdAt;

  @Data
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Key {
    String accountId;
    String orgIdentifier;
    String projectIdentifier;
    String environmentIdentifier;
    String fromServiceIdentifier;
    String toServiceIdentifier;
  }

  @JsonIgnore
  public Key getKey() {
    return Key.builder()
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .environmentIdentifier(environmentIdentifier)
        .fromServiceIdentifier(fromServiceIdentifier)
        .toServiceIdentifier(toServiceIdentifier)
        .build();
  }
}

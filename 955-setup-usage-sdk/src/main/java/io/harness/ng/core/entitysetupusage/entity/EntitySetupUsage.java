package io.harness.ng.core.entitysetupusage.entity;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.EntityDetail.EntityDetailKeys;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetail;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "EntitySetupUsageKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("entitySetupUsage")
@TypeAlias("io.harness.ng.core.entityReference.entity.EntitySetupUsage")
@OwnedBy(DX)
public class EntitySetupUsage implements PersistentEntity, NGAccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("ReferredByEntityIndex")
                 .field(EntitySetupUsageKeys.referredByEntityType)
                 .field(EntitySetupUsageKeys.referredByEntityFQN)
                 .field(EntitySetupUsageKeys.accountIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("ReferredEntityIndex")
                 .field(EntitySetupUsageKeys.referredEntityType)
                 .field(EntitySetupUsageKeys.referredEntityFQN)
                 .field(EntitySetupUsageKeys.accountIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("EntitySetupUsage_unique_index")
                 .field(EntitySetupUsageKeys.referredByEntityType)
                 .field(EntitySetupUsageKeys.referredByEntityFQN)
                 .field(EntitySetupUsageKeys.referredByEntityRepoIdentifier)
                 .field(EntitySetupUsageKeys.referredByEntityBranch)
                 .field(EntitySetupUsageKeys.referredEntityType)
                 .field(EntitySetupUsageKeys.referredEntityFQN)
                 .field(EntitySetupUsageKeys.referredEntityRepoIdentifier)
                 .field(EntitySetupUsageKeys.referredEntityBranch)
                 .field(EntitySetupUsageKeys.accountIdentifier)
                 .unique(true)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String id;
  @FdIndex @NotBlank String accountIdentifier;
  @NotNull EntityDetail referredEntity;
  @NotNull EntityDetail referredByEntity;
  SetupUsageDetail detail;

  @FdIndex @NotBlank String referredEntityFQN;
  @NotBlank String referredEntityType;
  String referredEntityRepoIdentifier;
  String referredEntityBranch;
  Boolean referredEntityIsDefault;

  @FdIndex @NotBlank String referredByEntityFQN;
  @NotBlank String referredByEntityType;
  String referredByEntityRepoIdentifier;
  String referredByEntityBranch;
  Boolean referredByEntityIsDefault;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  @CreatedBy private EmbeddedUser createdBy;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;

  @UtilityClass
  public static final class EntitySetupUsageKeys {
    public static final String referredEntityName = EntitySetupUsageKeys.referredEntity + "." + EntityDetailKeys.name;
    public static final String referredByEntityName =
        EntitySetupUsageKeys.referredByEntity + "." + EntityDetailKeys.name;
  }
}

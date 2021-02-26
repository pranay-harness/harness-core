package io.harness.pms.ngpipeline.inputset.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import com.sun.istack.internal.NotNull;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "InputSetEntityKeys")
@Entity(value = "inputSetsPMS", noClassnameStored = true)
@Document("inputSetsPMS")
@TypeAlias("inputSetsPMS")
@HarnessEntity(exportable = true)
public class InputSetEntity implements PersistentEntity, AccountAccess, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_organizationId_projectId_pipelineId_inputSetId")
                 .unique(true)
                 .field(InputSetEntityKeys.accountId)
                 .field(InputSetEntityKeys.orgIdentifier)
                 .field(InputSetEntityKeys.projectIdentifier)
                 .field(InputSetEntityKeys.pipelineIdentifier)
                 .field(InputSetEntityKeys.identifier)
                 .build())
        .add(CompoundMongoIndex.builder().name("accountIdIndex").field(InputSetEntityKeys.accountId).build())
        .build();
  }
  @Id @org.mongodb.morphia.annotations.Id String uuid;

  @NotEmpty String yaml;

  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Trimmed @NotEmpty String pipelineIdentifier;

  @NotEmpty String identifier;
  @EntityName String name;
  @Size(max = 1024) String description;
  @Singular @Size(max = 128) List<NGTag> tags;

  @NotEmpty InputSetEntityType inputSetEntityType;
  List<String> inputSetReferences;

  @SchemaIgnore @FdIndex @CreatedDate private long createdAt;
  @SchemaIgnore @NotNull @LastModifiedDate private long lastUpdatedAt;
  @Builder.Default Boolean deleted = Boolean.FALSE;
  @Version Long version;

  @Override
  public String getAccountId() {
    return accountId;
  }
}

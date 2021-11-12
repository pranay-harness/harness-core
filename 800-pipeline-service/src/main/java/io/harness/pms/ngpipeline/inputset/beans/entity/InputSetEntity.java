package io.harness.pms.ngpipeline.inputset.beans.entity;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
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
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "InputSetEntityKeys")
@Entity(value = "inputSetsPMS", noClassnameStored = true)
@Document("inputSetsPMS")
@TypeAlias("inputSetsPMS")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.PMS)
public class InputSetEntity
    implements GitSyncableEntity, PersistentEntity, AccountAccess, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_organizationId_projectId_pipelineId_inputSetId_repo_branch")
                 .unique(true)
                 .field(InputSetEntityKeys.accountId)
                 .field(InputSetEntityKeys.orgIdentifier)
                 .field(InputSetEntityKeys.projectIdentifier)
                 .field(InputSetEntityKeys.pipelineIdentifier)
                 .field(InputSetEntityKeys.identifier)
                 .field(InputSetEntityKeys.yamlGitConfigRef)
                 .field(InputSetEntityKeys.branch)
                 .build())
        .build();
  }
  @Setter @NonFinal @Id @org.mongodb.morphia.annotations.Id String uuid;

  @Wither @NotEmpty String yaml;

  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Trimmed @NotEmpty String pipelineIdentifier;

  @NotEmpty String identifier;
  @Wither @EntityName String name;
  @Wither @Size(max = 1024) String description;
  @Wither @Singular @Size(max = 128) List<NGTag> tags;

  @NotEmpty InputSetEntityType inputSetEntityType;
  @Wither List<String> inputSetReferences;

  @Setter @NonFinal @SchemaIgnore @FdIndex @CreatedDate long createdAt;
  @Setter @NonFinal @SchemaIgnore @NotNull @LastModifiedDate long lastUpdatedAt;
  @Wither @Builder.Default Boolean deleted = Boolean.FALSE;
  @Wither @Version Long version;

  @Setter @NonFinal String objectIdOfYaml;
  @Setter @NonFinal Boolean isFromDefaultBranch;
  @Setter @NonFinal String branch;
  @Setter @NonFinal String yamlGitConfigRef;
  @Setter @NonFinal String filePath;
  @Setter @NonFinal String rootFolder;
  @NonFinal Boolean isEntityInvalid;

  @Wither @Builder.Default Boolean isInvalid = Boolean.FALSE;

  @Override
  public String getAccountIdentifier() {
    return accountId;
  }

  @NonNull
  public Boolean getIsInvalid() {
    if (isInvalid == null) {
      return Boolean.FALSE;
    }
    return isInvalid;
  }

  @Override
  public boolean isEntityInvalid() {
    return Boolean.TRUE.equals(isEntityInvalid);
  }

  @Override
  public void setEntityInvalid(boolean isEntityInvalid) {
    this.isEntityInvalid = isEntityInvalid;
  }

  @Override
  public String getInvalidYamlString() {
    return yaml;
  }
}

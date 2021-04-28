package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("yamlGitConfigs")
@TypeAlias("io.harness.gitsync.common.beans.yamlGitConfigs")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "yamlGitConfigs", noClassnameStored = true)
@OwnedBy(DX)
@FieldNameConstants(innerTypeName = "YamlGitConfigKeys")
public class YamlGitConfig implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                      UpdatedByAware, AccountAccess {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  @NotEmpty @EntityIdentifier private String identifier;
  @NotEmpty private String name;
  @Trimmed @NotEmpty private String accountId;
  private String projectIdentifier;
  private String orgIdentifier;
  @NotEmpty String gitConnectorRef;
  @NotEmpty String repo;
  @NotEmpty String branch;
  @NotEmpty String webhookToken;
  Scope scope;
  List<YamlGitConfigDTO.RootFolder> rootFolders;
  YamlGitConfigDTO.RootFolder defaultRootFolder;
  @NotNull private ConnectorType gitConnectorType;
  Boolean executeOnDelegate;
  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;
  @Version Long version;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_gitConnectorId_repo_branch_unique_Index")
                 .fields(Arrays.asList(YamlGitConfigKeys.accountId, YamlGitConfigKeys.orgIdentifier,
                     YamlGitConfigKeys.projectIdentifier, YamlGitConfigKeys.gitConnectorRef, YamlGitConfigKeys.repo,
                     YamlGitConfigKeys.branch))
                 .unique(true)
                 .build())
        .build();
  }
}

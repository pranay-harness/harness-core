package io.harness.gitsync.common.beans;

import static io.harness.delegate.beans.git.EntityScope.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.ng.core.ProjectAccess;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
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
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document("gitFileLocation")
@TypeAlias("io.harness.gitsync.common.beans.gitFileLocation")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "gitFileLocation", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "GitFileLocationKeys")
public class GitFileLocation implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                        UpdatedByAware, AccountAccess, ProjectAccess {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  private String yamlGitFolderConfigId;
  private String entityGitPath;
  private String entityIdentifier;
  private String entityRootFolderName;
  private String entityRootFolderId;
  private String entityIdentifierFQN;
  private String entityType;
  private String projectId;
  private String organizationId;
  @Trimmed @NotEmpty private String accountId;

  Scope scope;

  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;
}
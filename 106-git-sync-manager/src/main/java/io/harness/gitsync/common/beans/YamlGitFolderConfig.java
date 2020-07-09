package io.harness.gitsync.common.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityIdentifier;
import io.harness.gitsync.common.EntityScope.Scope;
import io.harness.ng.ProjectAccess;
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
@Document("yamlGitFolderConfigs")
@TypeAlias("yamlGitFolderConfigs")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "yamlGitFolderConfig", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "YamlGitFolderConfigKeys")
public class YamlGitFolderConfig implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                            UpdatedByAware, AccountAccess, ProjectAccess {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id @EntityIdentifier private String uuid;
  private String yamlGitConfigId;
  @NotEmpty private String gitConnectorId;
  @NotEmpty private String repo;
  @NotEmpty private String branch;
  @NotEmpty private String rootFolder;
  @NotEmpty boolean isDefault;
  private boolean enabled;
  private String projectId;
  private String organizationId;
  private String accountId;
  private Scope scope;

  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;
}

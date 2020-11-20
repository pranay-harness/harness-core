package io.harness.gitsync.gitfileactivity.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.git.model.ChangeType;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.core.OrganizationAccess;
import io.harness.ng.core.ProjectAccess;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitFileActivityKeys")
@Entity(value = "gitFileActivity")
@HarnessEntity(exportable = false)
@Document("gitFileActivity")
@TypeAlias("io.harness.gitsync.gitfileactivity.beans.gitFileActivity")
public class GitFileActivity implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess,
                                        OrganizationAccess, ProjectAccess {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  private String accountId;
  private String organizationId;
  private String projectId;
  private String filePath;
  private String rootFilePath;
  private String fileContent;
  private String commitId;
  private String processingCommitId;
  private ChangeType changeType;
  private String errorMessage;
  private Status status;
  private TriggeredBy triggeredBy;
  private boolean changeFromAnotherCommit;
  private String commitMessage;
  private String processingCommitMessage;
  private long createdAt;
  @FdIndex private long lastUpdatedAt;
  private String gitConnectorId;
  private String repo;
  private String rootFolder;
  private String branchName;

  public enum Status { SUCCESS, FAILED, DISCARDED, EXPIRED, SKIPPED, QUEUED }

  public enum TriggeredBy { USER, GIT, FULL_SYNC }
}

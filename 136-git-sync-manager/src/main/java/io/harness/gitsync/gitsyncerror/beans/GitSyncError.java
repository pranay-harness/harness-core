package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.common.EntityReference;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails.GitToHarnessErrorDetailsKeys;
import io.harness.gitsync.gitsyncerror.beans.HarnessToGitErrorDetails.HarnessToGitErrorDetailsKeys;
import io.harness.ng.DbAliases;
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
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@EqualsAndHashCode()
@FieldNameConstants(innerTypeName = "GitSyncErrorKeys")
@Entity(value = "gitSyncErrorNG")
@HarnessEntity(exportable = false)
@Document("gitSyncErrorNG")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.gitSyncError")
@OwnedBy(DX)
@StoreIn(DbAliases.NG_MANAGER)
public class GitSyncError
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  // The project details of the file
  private String accountIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;

  // The repo details about the git sync error repo
  private String yamlGitConfigRef;
  private String branchName;
  private String repoURL;

  // The details about the file in git
  private ChangeType changeType;
  private String rootFolder;
  private String filePath;
  private String completeFilePath;

  // The entity details
  private EntityType entityType;
  private EntityReference entityReference;

  // The error details
  private String failureReason;
  private GitSyncErrorStatus status;
  private GitSyncErrorType errorType;
  private GitSyncErrorDetails additionalErrorDetails;

  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;

  @UtilityClass
  public static final class GitSyncErrorKeys {
    public static final String gitCommitId =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.gitCommitId;
    public static final String commitTime =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.commitTime;
    public static final String fullSyncPath =
        GitSyncErrorKeys.additionalErrorDetails + "." + HarnessToGitErrorDetailsKeys.fullSyncPath;
    public static final String commitMessage =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.commitMessage;
  }
}

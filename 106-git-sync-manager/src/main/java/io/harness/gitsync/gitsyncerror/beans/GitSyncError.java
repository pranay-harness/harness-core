package io.harness.gitsync.gitsyncerror.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.delegate.beans.git.EntityScope.Scope;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails.GitToHarnessErrorDetailsKeys;
import io.harness.gitsync.gitsyncerror.beans.HarnessToGitErrorDetails.HarnessToGitErrorDetailsKeys;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.core.OrganizationAccess;
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
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.ws.rs.DefaultValue;

@Data
@NoArgsConstructor
@EqualsAndHashCode()
@FieldNameConstants(innerTypeName = "GitSyncErrorKeys")
@Entity(value = "gitSyncError")
@HarnessEntity(exportable = false)
@Document("gitSyncError")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.gitSyncError")
public class GitSyncError
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware,
               AccountAccess, OrganizationAccess, ProjectAccess, PersistentRegularIterable {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  private String accountId;
  private String projectId;
  private String organizationId;
  private String yamlFilePath;
  private String changeType;
  private String failureReason;
  @Setter @FdIndex private Long nextIteration;
  private boolean fullSyncPath;
  private GitSyncErrorStatus status;
  private String gitConnectorId;
  @Transient private String gitConnectorName;
  private String branchName;
  private String repo;
  private String rootFolder;
  private String yamlGitConfigId;
  private GitSyncErrorDetails additionalErrorDetails;
  private GitSyncDirection gitSyncDirection;
  private Scope errorEntityType;
  @Transient @DefaultValue("false") private boolean userDoesNotHavePermForFile;

  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;

  @Builder
  public GitSyncError(String accountId, String organizationId, String projectId, String yamlFilePath, String changeType,
      String failureReason, String gitConnectorId, String branchName, String yamlGitConfigId,
      GitSyncErrorDetails additionalErrorDetails, GitSyncDirection gitSyncDirection, GitSyncErrorStatus status,
      boolean userDoesNotHavePermForFile, boolean fullSyncPath, Scope errorEntityType, String repo, String rootFolder) {
    this.accountId = accountId;
    this.organizationId = organizationId;
    this.projectId = projectId;
    this.yamlFilePath = yamlFilePath;
    this.changeType = changeType;
    this.failureReason = failureReason;
    this.gitConnectorId = gitConnectorId;
    this.branchName = branchName;
    this.yamlGitConfigId = yamlGitConfigId;
    this.additionalErrorDetails = additionalErrorDetails;
    this.gitSyncDirection = gitSyncDirection;
    this.fullSyncPath = fullSyncPath;
    this.status = status;
    this.userDoesNotHavePermForFile = userDoesNotHavePermForFile;
    this.repo = repo;
    this.rootFolder = rootFolder;
    this.errorEntityType = errorEntityType;
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @UtilityClass
  public static final class GitSyncErrorKeys {
    public static final String gitCommitId =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.gitCommitId;
    public static final String commitTime =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.commitTime;
    public static final String fullSyncPath =
        GitSyncErrorKeys.additionalErrorDetails + "." + HarnessToGitErrorDetailsKeys.fullSyncPath;
    public static final String previousCommitIds =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.previousCommitIdsWithError;
    public static final String commitMessage =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.commitMessage;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  public enum GitSyncDirection { GIT_TO_HARNESS, HARNESS_TO_GIT }
}

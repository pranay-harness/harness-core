package io.harness.gitsync.core.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "gitCommitNG", noClassnameStored = true)
@Document("gitCommitNG")
@TypeAlias("io.harness.gitsync.core.beans.gitCommit")
@FieldNameConstants(innerTypeName = "GitCommitKeys")
@OwnedBy(DX)
public class GitCommit
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  private String accountIdentifier;
  private String commitId;
  @FdIndex private GitCommitProcessingStatus status;
  private FailureReason failureReason;
  private GitFileProcessingSummary fileProcessingSummary;
  private String commitMessage;
  private String gitConnectorId;
  private String repoURL;
  private String branchName;
  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;

  public enum GitCommitProcessingStatus { QUEUED, RUNNING, COMPLETED, FAILED, COMPLETED_WITH_ERRORS, SKIPPED }

  public enum FailureReason {
    GIT_CONNECTION_FAILED,
    GIT_CLONE_FAILED,
    GIT_PUSH_FAILED,
    GIT_PULL_FAILED,
    COMMIT_PARSING_FAILED
  }

  public static final List<GitCommitProcessingStatus> GIT_COMMIT_PROCESSED_STATUS =
      ImmutableList.of(GitCommitProcessingStatus.COMPLETED, GitCommitProcessingStatus.COMPLETED_WITH_ERRORS);

  public static final List<GitCommitProcessingStatus> GIT_COMMIT_ALL_STATUS_LIST =
      ImmutableList.<GitCommitProcessingStatus>builder()
          .addAll(GIT_COMMIT_PROCESSED_STATUS)
          .add(GitCommitProcessingStatus.FAILED)
          .add(GitCommitProcessingStatus.SKIPPED)
          .build();
}

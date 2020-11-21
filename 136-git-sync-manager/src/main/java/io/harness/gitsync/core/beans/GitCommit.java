package io.harness.gitsync.core.beans;

import io.harness.beans.EmbeddedUser;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
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
import lombok.Singular;
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
@Entity(value = "gitCommit", noClassnameStored = true)
@Document("gitCommit")
@TypeAlias("io.harness.gitsync.core.beans.gitCommit")
@FieldNameConstants(innerTypeName = "GitCommitKeys")
public class GitCommit implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                  UpdatedByAware, AccountAccess {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  private String accountId;
  @Singular("yamlGitConfigIds") private List<String> yamlGitConfigIds;
  private String commitId;
  private String yamlChangeSetId;
  @FdIndex private Status status;
  private FailureReason failureReason;
  private GitFileProcessingSummary fileProcessingSummary;
  private String commitMessage;
  private String gitConnectorId;
  private String repo;
  private String branchName;
  private String projectId;
  private String organizationId;
  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;

  public enum Status { QUEUED, RUNNING, COMPLETED, FAILED, COMPLETED_WITH_ERRORS, SKIPPED }

  public enum FailureReason {
    GIT_CONNECTION_FAILED,
    GIT_CLONE_FAILED,
    GIT_PUSH_FAILED,
    GIT_PULL_FAILED,
    COMMIT_PARSING_FAILED
  }

  public static final List<Status> GIT_COMMIT_PROCESSED_STATUS =
      ImmutableList.of(Status.COMPLETED, Status.COMPLETED_WITH_ERRORS);

  public static final List<Status> GIT_COMMIT_ALL_STATUS_LIST = ImmutableList.<Status>builder()
                                                                    .addAll(GIT_COMMIT_PROCESSED_STATUS)
                                                                    .add(Status.FAILED)
                                                                    .add(Status.SKIPPED)
                                                                    .build();
}

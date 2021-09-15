/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.EntityType;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.common.EntityReference;
import io.harness.data.validator.Trimmed;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails.GitToHarnessErrorDetailsKeys;
import io.harness.gitsync.gitsyncerror.beans.HarnessToGitErrorDetails.HarnessToGitErrorDetailsKeys;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

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
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@EqualsAndHashCode()
@FieldNameConstants(innerTypeName = "GitSyncErrorKeys")
@Entity(value = "gitSyncErrorNG")
@HarnessEntity(exportable = false)
@Document("gitSyncErrorNG")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.gitSyncError")
@OwnedBy(PL)
@StoreIn(DbAliases.NG_MANAGER)
public class GitSyncError
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  @Trimmed @NotEmpty private String accountIdentifier;
  // The repo details about the git sync error repo
  private String repoUrl;
  private String branchName;

  // The details about the file in git
  private ChangeType changeType;
  private String completeFilePath;

  // The entity details
  private EntityType entityType;
  private EntityReference entityReference;

  // The error details
  @NotEmpty private String failureReason;
  @NotNull private GitSyncErrorStatus status;
  @NotNull private GitSyncErrorType errorType;
  private GitSyncErrorDetails additionalErrorDetails;

  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;

  public static final class GitSyncErrorKeys {
    public static final String gitCommitId =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.gitCommitId;
    public static final String commitTime =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.commitTime;
    public static final String commitMessage =
        GitSyncErrorKeys.additionalErrorDetails + "." + GitToHarnessErrorDetailsKeys.commitMessage;
    public static final String orgIdentifier =
        GitSyncErrorKeys.additionalErrorDetails + "." + HarnessToGitErrorDetailsKeys.orgIdentifier;
    public static final String projectIdentifier =
        GitSyncErrorKeys.additionalErrorDetails + "." + HarnessToGitErrorDetailsKeys.projectIdentifier;
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList
        .<MongoIndex>builder()
        // for gitToHarness errors
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_errorType_repo_branch_sort_Index")
                 .fields(Arrays.asList(GitSyncErrorKeys.accountIdentifier, GitSyncErrorKeys.errorType,
                     GitSyncErrorKeys.repoUrl, GitSyncErrorKeys.branchName))
                 .descSortField(GitSyncErrorKeys.createdAt)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("accountId_commitId_repo_branch_filePath_unique_Index")
                 .fields(Arrays.asList(GitSyncErrorKeys.accountIdentifier, GitSyncErrorKeys.gitCommitId,
                     GitSyncErrorKeys.repoUrl, GitSyncErrorKeys.branchName, GitSyncErrorKeys.completeFilePath))
                 .unique(true)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("accountId_repo_branch_filePath_Index")
                 .fields(Arrays.asList(GitSyncErrorKeys.accountIdentifier, GitSyncErrorKeys.repoUrl,
                     GitSyncErrorKeys.branchName, GitSyncErrorKeys.completeFilePath))
                 .build())
        // for full sync and connectivity issue errors
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_errorType_repo_branch_sort_Index")
                 .fields(Arrays.asList(GitSyncErrorKeys.accountIdentifier, GitSyncErrorKeys.orgIdentifier,
                     GitSyncErrorKeys.projectIdentifier, GitSyncErrorKeys.errorType, GitSyncErrorKeys.repoUrl,
                     GitSyncErrorKeys.branchName))
                 .descSortField(GitSyncErrorKeys.createdAt)
                 .build())
        .build();
  }
}

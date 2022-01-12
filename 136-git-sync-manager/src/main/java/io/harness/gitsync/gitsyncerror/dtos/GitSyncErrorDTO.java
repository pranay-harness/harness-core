/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "GitSyncError", description = "This contains Git Sync Error Details")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@OwnedBy(PL)
public class GitSyncErrorDTO {
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) String accountIdentifier;
  @Schema(description = GitSyncApiConstants.REPO_URL_PARAM_MESSAGE) String repoUrl;
  @Schema(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) String repoId;
  @Schema(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE) String branchName;
  @Schema(description = "List of scope of the Git Sync Error") List<Scope> scopes;
  @Schema(description = "Type of operation done in file") ChangeType changeType;
  @Schema(description = "Complete File Path of the Entity") String completeFilePath;
  @Schema(description = GitSyncApiConstants.ENTITY_TYPE_PARAM_MESSAGE) EntityType entityType;

  @Schema(description = "Error Message") String failureReason;
  @Schema(description = "Status of Git Sync Error") GitSyncErrorStatus status;
  @Schema(description = "Type of Git Sync Error") GitSyncErrorType errorType;
  @Schema(description = "Additional Details of Git Sync Error based on its type")
  GitSyncErrorDetailsDTO additionalErrorDetails;

  @Schema(description = "Time at which the Git Sync error was logged") long createdAt;
}

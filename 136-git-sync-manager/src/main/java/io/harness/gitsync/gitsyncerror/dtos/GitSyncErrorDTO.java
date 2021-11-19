package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
  String accountIdentifier;
  String repoUrl;
  String repoId;
  String branchName;
  ChangeType changeType;
  String completeFilePath;
  EntityType entityType;

  String failureReason;
  GitSyncErrorStatus status;
  GitSyncErrorType errorType;
  GitSyncErrorDetailsDTO additionalErrorDetails;

  long createdAt;
}

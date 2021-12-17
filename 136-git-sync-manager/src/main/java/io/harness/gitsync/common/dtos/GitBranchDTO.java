package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.gitsync.common.beans.BranchSyncStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "SyncedBranchDTOKeys")
@Schema(name = "GitBranch", description = "This contains details of the branch")
@OwnedBy(DX)
public class GitBranchDTO {
  @Trimmed @NotEmpty private String branchName;
  @NotEmpty private BranchSyncStatus branchSyncStatus;
}

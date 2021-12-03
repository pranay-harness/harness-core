package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "GitBranchList", description = "This contains the details of branches and the default branch")
@OwnedBy(DX)
public class GitBranchListDTO {
  GitBranchDTO defaultBranch;
  PageResponse<GitBranchDTO> branches;
}

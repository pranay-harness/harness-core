package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class GitBranchListDTO {
  GitBranchDTO defaultBranch;
  PageResponse<GitBranchDTO> branches;
}

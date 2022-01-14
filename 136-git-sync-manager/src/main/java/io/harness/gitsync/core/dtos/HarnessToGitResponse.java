package io.harness.gitsync.core.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class HarnessToGitResponse {
  private String status;
  private String errorMsg;

  public enum Status { SUCCESS, FAILED }
}

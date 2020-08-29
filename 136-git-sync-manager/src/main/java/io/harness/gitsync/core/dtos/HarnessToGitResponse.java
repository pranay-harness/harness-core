package io.harness.gitsync.core.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HarnessToGitResponse {
  private String status;
  private String errorMsg;

  public enum Status { SUCCESS, FAILED }
}

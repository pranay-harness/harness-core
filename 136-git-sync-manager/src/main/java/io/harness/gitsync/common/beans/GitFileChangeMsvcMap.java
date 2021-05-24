package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.GitFileChange;

import javax.validation.constraints.NotNull;

@OwnedBy(DX)
public class GitFileChangeMsvcMap {
  public static final String UNKNOWN_MSVC = "UNKNOWN_MSVC";
  @NotNull private String microservice;
  @NotNull private GitFileChange gitFileChange;
  @NotNull private String status;

  public enum Status { QUEUED, SUCCESS, ERROR_IN_PARSE, ERROR }
}

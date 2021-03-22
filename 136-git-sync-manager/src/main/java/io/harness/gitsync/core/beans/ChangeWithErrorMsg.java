package io.harness.gitsync.core.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.GitFileChange;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class ChangeWithErrorMsg {
  private GitFileChange change;
  private String errorMsg;
}

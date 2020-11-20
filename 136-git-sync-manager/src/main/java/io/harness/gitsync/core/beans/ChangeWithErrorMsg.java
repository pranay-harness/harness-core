package io.harness.gitsync.core.beans;

import io.harness.git.model.GitFileChange;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangeWithErrorMsg {
  private GitFileChange change;
  private String errorMsg;
}

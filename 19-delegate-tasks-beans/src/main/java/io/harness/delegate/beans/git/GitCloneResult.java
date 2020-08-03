package io.harness.delegate.beans.git;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GitCloneResult extends GitCommandResult {
  public GitCloneResult() {
    super(GitCommandType.CLONE);
  }
}

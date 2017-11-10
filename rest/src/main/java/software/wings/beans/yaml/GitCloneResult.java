package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 10/16/17.
 */

@Data
@Builder
public class GitCloneResult extends GitCommandResult {
  public GitCloneResult() {
    super(GitCommandType.CLONE);
  }
}

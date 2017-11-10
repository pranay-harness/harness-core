package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@Builder
public class GitPullResult extends GitCommandResult {
  public GitPullResult() {
    super(GitCommandType.PULL);
  }
}

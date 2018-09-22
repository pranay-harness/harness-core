package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitPullResult extends GitCommandResult {
  public GitPullResult() {
    super(GitCommandType.PULL);
  }
}

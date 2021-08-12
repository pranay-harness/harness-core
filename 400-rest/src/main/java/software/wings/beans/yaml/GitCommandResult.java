package software.wings.beans.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

/**
 * Created by anubhaw on 10/27/17.
 */
@TargetModule(HarnessModule._870_YAML_BEANS)
public class GitCommandResult implements GitCommand {
  private GitCommandType gitCommandType;

  public GitCommandResult(GitCommandType gitCommandType) {
    this.gitCommandType = gitCommandType;
  }

  @Override
  public GitCommandType getGitCommandType() {
    return gitCommandType;
  }
}

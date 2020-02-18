package software.wings.beans.yaml;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 10/27/17.
 */
public class GitCommandRequest implements GitCommand {
  protected GitCommandType gitCommandType;
  public static final long gitRequestTimeout = TimeUnit.MINUTES.toMillis(20);

  public GitCommandRequest(GitCommandType gitCommandType) {
    this.gitCommandType = gitCommandType;
  }

  @Override
  public GitCommandType getGitCommandType() {
    return gitCommandType;
  }
}

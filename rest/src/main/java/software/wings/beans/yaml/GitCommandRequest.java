package software.wings.beans.yaml;

/**
 * Created by anubhaw on 10/27/17.
 */
public class GitCommandRequest implements GitCommand {
  protected GitCommandType gitCommandType;

  public GitCommandRequest(GitCommandType gitCommandType) {
    this.gitCommandType = gitCommandType;
  }

  @Override
  public GitCommandType getGitCommandType() {
    return gitCommandType;
  }
}

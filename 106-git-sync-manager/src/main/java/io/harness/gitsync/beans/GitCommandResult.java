package io.harness.gitsync.beans;

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

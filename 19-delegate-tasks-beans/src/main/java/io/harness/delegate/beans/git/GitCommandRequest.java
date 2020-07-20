package io.harness.delegate.beans.git;

import java.util.concurrent.TimeUnit;

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
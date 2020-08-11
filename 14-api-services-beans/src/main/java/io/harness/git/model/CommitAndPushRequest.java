package io.harness.git.model;

import java.util.List;

public class CommitAndPushRequest extends GitBaseRequest {
  private String lastProcessedGitCommit;
  private boolean pushOnlyIfHeadSeen;
  private List<?> gitFileChangeList;
  private boolean forcePush;

  CommitAndPushRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest, String connectorId,
      String accountId, String repoType, String lastProcessedGitCommit, boolean pushOnlyIfHeadSeen,
      List<?> gitFileChangeList, boolean forcePush) {
    super(repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType);
    this.lastProcessedGitCommit = lastProcessedGitCommit;
    this.pushOnlyIfHeadSeen = pushOnlyIfHeadSeen;
    this.gitFileChangeList = gitFileChangeList;
    this.forcePush = forcePush;
  }
}

package io.harness.git.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommitAndPushRequest extends GitBaseRequest {
  private String lastProcessedGitCommit;
  private boolean pushOnlyIfHeadSeen;
  private List<GitFileChange> gitFileChanges;
  private boolean forcePush;
  private String commitMessage;
  private String authorName;
  private String authorEmail;

  @Builder(builderMethodName = "commitAndPushRequestBuilder")
  public CommitAndPushRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest,
      String connectorId, String accountId, String repoType, String lastProcessedGitCommit, boolean pushOnlyIfHeadSeen,
      List<GitFileChange> gitFileChanges, boolean forcePush, String commitMessage, String authorName,
      String authorEmail) {
    super(repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType);
    this.lastProcessedGitCommit = lastProcessedGitCommit;
    this.pushOnlyIfHeadSeen = pushOnlyIfHeadSeen;
    this.gitFileChanges = gitFileChanges;
    this.forcePush = forcePush;
    this.commitMessage = commitMessage;
    this.authorName = authorName;
    this.authorEmail = authorEmail;
  }
}

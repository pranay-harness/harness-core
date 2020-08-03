package io.harness.cdng.gitclient;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCheckoutResult;
import io.harness.delegate.beans.git.GitCloneResult;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.GitCommitAndPushResult;
import io.harness.delegate.beans.git.GitCommitResult;
import io.harness.delegate.beans.git.GitPushResult;

public interface GitClientNG {
  String validate(GitConfigDTO gitConfig);

  GitCommitAndPushResult commitAndPush(
      GitConfigDTO gitConfigDTO, GitCommitAndPushRequest gitCommitRequest, String accountId, String reference);

  GitCommitResult commit(
      GitConfigDTO gitConfig, GitCommitAndPushRequest gitCommitRequest, String accountId, String reference);

  GitCloneResult clone(GitConfigDTO gitConfig, String gitRepoDirectory, String branch, boolean noCheckout);

  GitCheckoutResult checkout(GitConfigDTO gitConfig, String accountId, String reference);

  GitPushResult push(GitConfigDTO gitConfig, GitCommitAndPushRequest gitCommitRequest, String accountId);
}

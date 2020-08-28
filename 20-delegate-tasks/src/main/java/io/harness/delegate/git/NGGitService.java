package io.harness.delegate.git;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;

public interface NGGitService {
  String validate(GitConfigDTO gitConfig, String accountId);

  CommitAndPushResult commitAndPush(
      GitConfigDTO gitConfig, CommitAndPushRequest commitAndPushRequest, String accountId);
}

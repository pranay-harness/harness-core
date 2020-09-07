package io.harness.delegate.git;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.FetchFilesResult;

public interface NGGitService {
  String validate(GitConfigDTO gitConfig, String accountId);

  CommitAndPushResult commitAndPush(
      GitConfigDTO gitConfig, CommitAndPushRequest commitAndPushRequest, String accountId);

  FetchFilesResult fetchFilesByPath(GitStoreDelegateConfig gitStoreDelegateConfig, String accountId);

  void downloadFiles(GitStoreDelegateConfig gitStoreDelegateConfig, String manifestFilesDirectory, String accountId);
}

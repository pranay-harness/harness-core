package io.harness.delegate.task.gitapi.client;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.gitapi.GitApiTaskParams;

public interface GitApiClient {
  DelegateResponseData findPullRequest(GitApiTaskParams gitApiTaskParams);
}

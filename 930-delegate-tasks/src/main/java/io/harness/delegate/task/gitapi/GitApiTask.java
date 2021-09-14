/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.gitapi;

import static io.harness.delegate.beans.gitapi.GitApiRequestType.FIND_PULL_REQUEST_DETAILS;
import static io.harness.delegate.beans.gitapi.GitRepoType.GITHUB;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.gitapi.GitApiTaskParams;
import io.harness.delegate.beans.gitapi.GitApiTaskResponse;
import io.harness.delegate.beans.gitapi.GitRepoType;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.gitapi.client.GitApiClient;
import io.harness.delegate.task.gitapi.client.impl.GithubApiClient;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@Slf4j
public class GitApiTask extends AbstractDelegateRunnableTask {
  @Inject private GithubApiClient githubApiClient;

  public GitApiTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    GitApiTaskParams gitApiTaskParams = (GitApiTaskParams) parameters;
    if (gitApiTaskParams.getRequestType() == FIND_PULL_REQUEST_DETAILS) {
      try {
        return getGitApiClient(gitApiTaskParams.getGitRepoType()).findPullRequest(gitApiTaskParams);
      } catch (Exception ex) {
        log.error("failed to send status", ex);
        return GitApiTaskResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(ex.getMessage())
            .build();
      }
    }

    throw new UnsupportedOperationException("Unknown Request Type: " + gitApiTaskParams.getRequestType());
  }

  private GitApiClient getGitApiClient(GitRepoType gitRepoType) {
    if (gitRepoType == GITHUB) {
      return githubApiClient;
    }

    throw new UnsupportedOperationException("Only Github Api client has benn added so far");
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}

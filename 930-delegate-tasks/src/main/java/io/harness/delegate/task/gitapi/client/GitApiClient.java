/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.gitapi.client;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.gitapi.GitApiTaskParams;

public interface GitApiClient {
  DelegateResponseData findPullRequest(GitApiTaskParams gitApiTaskParams);
}

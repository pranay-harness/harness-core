/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.gitapi;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DecryptGitAPIAccessTaskResponse implements DelegateResponseData {
  ScmConnector scmConnector;
}

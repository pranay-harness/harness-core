/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.gitapi;

import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DecryptGitAPiAccessTaskParams implements TaskParameters {
  ScmConnector scmConnector;
  List<EncryptedDataDetail> encryptedDataDetails;
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cistatus.service.gitlab;

import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

public interface GitlabService {
  boolean sendStatus(GitlabConfig bitbucketConfig, String userName, String token,
      List<EncryptedDataDetail> encryptionDetails, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap);
}

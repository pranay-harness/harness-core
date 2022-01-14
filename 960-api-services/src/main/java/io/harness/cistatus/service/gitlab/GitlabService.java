package io.harness.cistatus.service.gitlab;

import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

public interface GitlabService {
  boolean sendStatus(GitlabConfig bitbucketConfig, String userName, String token,
      List<EncryptedDataDetail> encryptionDetails, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap);
}

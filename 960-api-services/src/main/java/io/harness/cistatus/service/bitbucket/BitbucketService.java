package io.harness.cistatus.service.bitbucket;

import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

public interface BitbucketService {
  boolean sendStatus(BitbucketConfig bitbucketConfig, String userName, String token,
      List<EncryptedDataDetail> encryptionDetails, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap);
}

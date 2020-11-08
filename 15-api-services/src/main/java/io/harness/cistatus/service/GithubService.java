package io.harness.cistatus.service;

import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

public interface GithubService {
  String getToken(GithubAppConfig githubAppConfig, List<EncryptedDataDetail> encryptionDetails);

  boolean sendStatus(GithubAppConfig githubAppConfig, String token, List<EncryptedDataDetail> encryptionDetails,
      String sha, String owner, String repo, Map<String, Object> bodyObjectMap);
}

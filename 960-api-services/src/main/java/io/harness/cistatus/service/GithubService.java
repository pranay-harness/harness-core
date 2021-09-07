package io.harness.cistatus.service;

import java.util.Map;

public interface GithubService {
  String getToken(GithubAppConfig githubAppConfig);

  boolean sendStatus(GithubAppConfig githubAppConfig, String token, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap);

  String findPR(String apiUrl, String token, String owner, String repo, String prNumber);
}

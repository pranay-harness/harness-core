/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cistatus.service;

import java.util.Map;

public interface GithubService {
  String getToken(GithubAppConfig githubAppConfig);

  boolean sendStatus(GithubAppConfig githubAppConfig, String token, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap);

  String findPR(String apiUrl, String token, String owner, String repo, String prNumber);
}

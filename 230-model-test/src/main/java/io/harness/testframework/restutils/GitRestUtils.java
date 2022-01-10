/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.git.GitData;
import io.harness.testframework.framework.matchers.GitResponseMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class GitRestUtils {
  static final int MAX_RETRIES = 60;
  static final int DELAY_IN_MS = 6000;
  static Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);

  public static List<GitData> getGitAllForApp(String appName) {
    List<GitData> response =
        (List<GitData>) retry.executeWithRetry(() -> retriveGitResponse(appName), new GitResponseMatcher(), null);

    if (response.size() >= 2) {
      return response;
    } else {
      return getGitAllForApp(appName);
    }
  }

  public static List<GitData> getGitEnvForApp(String envName, String appName) {
    String var = appName + "/Environments/" + envName;
    return (List<GitData>) retry.executeWithRetry(() -> retriveGitResponse(var), new GitResponseMatcher(), null);
  }

  private static List<GitData> retriveGitResponse(String var) {
    String repoName = "automation-testing";
    String token = String.valueOf(new ScmSecret().decryptToCharArray(new SecretName("git_automation_token")));

    String response = Setup.git(repoName).header("Authorization", "Bearer " + token).get("/" + var).print();
    List<GitData> gitDataList = new ArrayList<>();
    JSONArray jsonArr = new JSONArray(response);

    for (Object object : jsonArr) {
      JSONObject jsonObj = (JSONObject) object;
      ObjectMapper mapper = new ObjectMapper();
      GitData gitData = null;
      try {
        gitData = mapper.readValue(jsonObj.toString(), GitData.class);
      } catch (IOException e) {
        log.error("", e);
      }
      gitDataList.add(gitData);
    }

    return gitDataList;
  }
}

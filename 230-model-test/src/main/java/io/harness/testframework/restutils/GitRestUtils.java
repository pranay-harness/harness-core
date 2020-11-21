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
import org.json.JSONArray;
import org.json.JSONObject;

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

    List<GitData> response =
        (List<GitData>) retry.executeWithRetry(() -> retriveGitResponse(var), new GitResponseMatcher(), null);

    return response;
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
        e.printStackTrace();
      }
      gitDataList.add(gitData);
    }

    return gitDataList;
  }
}

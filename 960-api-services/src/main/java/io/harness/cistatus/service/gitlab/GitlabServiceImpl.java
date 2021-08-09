package io.harness.cistatus.service.gitlab;

import static java.lang.String.format;

import io.harness.cistatus.StatusCreationResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Singleton
@Slf4j
public class GitlabServiceImpl implements GitlabService {
  private final int EXP_TIME = 5 * 60 * 1000;
  public static final String DESC = "description";
  public static final String STATE = "state";
  public static final String CONTEXT = "context";
  public static final String TARGET_URL = "target_url";
  private static final String SEPARATOR = "/";

  @Override
  public boolean sendStatus(GitlabConfig gitlabConfig, String userName, String token,
      List<EncryptedDataDetail> encryptionDetails, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap) {
    log.info("Sending status {} for sha {}", bodyObjectMap.get(STATE), sha);

    try {
      Response<StatusCreationResponse> statusCreationResponseResponse =
          getGitlabRestClient(gitlabConfig, encryptionDetails)
              .createStatus(getAuthToken(token), owner + SEPARATOR + repo, sha, (String) bodyObjectMap.get(STATE),
                  (String) bodyObjectMap.get(CONTEXT), (String) bodyObjectMap.get(DESC))
              .execute();

      return statusCreationResponseResponse.isSuccessful();
    } catch (Exception e) {
      log.error("Failed to post commit status request to Gitlab with url {} and sha {} ", gitlabConfig.getGitlabUrl(),
          sha, e);
      return false;
    }
  }

  @VisibleForTesting
  public GitlabRestClient getGitlabRestClient(GitlabConfig gitlabConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      String gitlabUrl = gitlabConfig.getGitlabUrl();
      Preconditions.checkNotNull(gitlabUrl, "Gitlab api url is null");
      if (!gitlabUrl.endsWith("/")) {
        gitlabUrl = gitlabUrl + "/";
      }
      Retrofit retrofit = new Retrofit.Builder()
                              .baseUrl(gitlabUrl)
                              .addConverterFactory(JacksonConverterFactory.create())
                              .client(Http.getUnsafeOkHttpClient(gitlabUrl))
                              .build();
      return retrofit.create(GitlabRestClient.class);
    } catch (InvalidRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(
          "Failed to post commit status request to github :" + gitlabConfig.getGitlabUrl(), e);
    }
  }

  private String getAuthToken(String authToken) {
    return format("Bearer %s", authToken);
  }
}

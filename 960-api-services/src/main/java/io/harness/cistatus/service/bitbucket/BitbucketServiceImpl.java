package io.harness.cistatus.service.bitbucket;

import static java.lang.String.format;

import io.harness.cistatus.StatusCreationResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Singleton
@Slf4j
public class BitbucketServiceImpl implements BitbucketService {
  private final int EXP_TIME = 5 * 60 * 1000;
  private static final String STATE = "state";

  @Override
  public boolean sendStatus(BitbucketConfig bitbucketConfig, String userName, String token,
      List<EncryptedDataDetail> encryptionDetails, String sha, String owner, String repo,
      Map<String, Object> bodyObjectMap) {
    log.info("Sending status {} for sha {}", bodyObjectMap.get(STATE), sha);

    try {
      Response<StatusCreationResponse> statusCreationResponseResponse;

      if (!bitbucketConfig.getBitbucketUrl().contains("bitbucket.org/")) {
        statusCreationResponseResponse =
            getBitbucketClient(bitbucketConfig, encryptionDetails)
                .createOnPremStatus(getHeaderWithCredentials(token, userName), sha, bodyObjectMap)
                .execute();
      } else {
        statusCreationResponseResponse =
            getBitbucketClient(bitbucketConfig, encryptionDetails)
                .createStatus(getHeaderWithCredentials(token, userName), owner, repo, sha, bodyObjectMap)
                .execute();
      }

      return statusCreationResponseResponse.isSuccessful();

    } catch (Exception e) {
      log.error("Failed to send status for Bitbucket url {} and sha {} ", bitbucketConfig.getBitbucketUrl(), sha, e);
      return false;
    }
  }

  @VisibleForTesting
  public BitbucketRestClient getBitbucketClient(
      BitbucketConfig bitbucketConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      String bitbucketUrl = bitbucketConfig.getBitbucketUrl();
      Preconditions.checkNotNull(bitbucketUrl, "Bitbucket api url is null");
      if (!bitbucketUrl.endsWith("/")) {
        bitbucketUrl = bitbucketUrl + "/";
      }
      Retrofit retrofit = new Retrofit.Builder()
                              .baseUrl(bitbucketUrl)
                              .addConverterFactory(JacksonConverterFactory.create())
                              .client(Http.getUnsafeOkHttpClient(bitbucketUrl))
                              .build();
      return retrofit.create(BitbucketRestClient.class);
    } catch (InvalidRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(
          "Failed to post commit status request to bitbucket server :" + bitbucketConfig.getBitbucketUrl(), e);
    }
  }

  private String getHeaderWithCredentials(String token, String userName) {
    return "Basic " + Base64.encodeBase64String(format("%s:%s", userName, token).getBytes(StandardCharsets.UTF_8));
  }
}

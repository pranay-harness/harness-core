package io.harness.cistatus.service;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.harness.cistatus.GithubAppTokenCreationResponse;
import io.harness.cistatus.GithubStatusCreationResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class GithubServiceImpl implements GithubService {
  private final int EXP_TIME = 5 * 60 * 1000;

  @Override
  public String getToken(GithubAppConfig githubAppConfig, List<EncryptedDataDetail> encryptionDetails) {
    log.info("Retrieving github installation token for installation id {}", githubAppConfig.getInstallationId());
    try {
      String jwtToken = generateTokenFromPrivateKey(githubAppConfig);
      GithubAppTokenCreationResponse response =
          executeRestCall(getGithubClient(githubAppConfig, encryptionDetails)
                              .createAccessToken(getAuthToken(jwtToken), githubAppConfig.getInstallationId()));
      return response.getToken();
    } catch (Exception e) {
      throw new InvalidRequestException("Failed to generate token :" + githubAppConfig.getGithubUrl(), e);
    }
  }

  @Override
  public boolean sendStatus(GithubAppConfig githubAppConfig, String token, List<EncryptedDataDetail> encryptionDetails,
      String sha, String owner, String repo, Map<String, Object> bodyObjectMap) {
    log.info("Sending status for sha {}", sha);

    try {
      Response<GithubStatusCreationResponse> githubStatusCreationResponseResponse =
          getGithubClient(githubAppConfig, encryptionDetails)
              .createStatus(getAuthToken(token), owner, repo, sha, bodyObjectMap)
              .execute();

      if (githubStatusCreationResponseResponse.isSuccessful()) {
        return true;
      } else {
        return false;
      }

    } catch (Exception e) {
      log.error("Failed to send status :" + githubAppConfig.getGithubUrl(), e);
      return false;
    }
  }

  @VisibleForTesting
  public GithubRestClient getGithubClient(
      GithubAppConfig githubAppConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      String githubUrl = githubAppConfig.getGithubUrl();
      if (githubUrl == null) {
        throw new InvalidRequestException("Invalid Github Url Server URL");
      }
      if (!githubUrl.endsWith("/")) {
        githubUrl = githubUrl + "/";
      }
      Retrofit retrofit = new Retrofit.Builder()
                              .baseUrl(githubUrl)
                              .addConverterFactory(JacksonConverterFactory.create())
                              .client(Http.getUnsafeOkHttpClient(githubUrl))
                              .build();
      return retrofit.create(GithubRestClient.class);
    } catch (InvalidRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Could not reach Github Server at :" + githubAppConfig.getGithubUrl(), e);
    }
  }

  private <T> T executeRestCall(Call<T> restRequest) throws Exception {
    Response<T> restResponse = restRequest.execute();
    if (!restResponse.isSuccessful()) {
      throw new InvalidRequestException(restResponse.errorBody().string(), EnumSet.of(ReportTarget.UNIVERSAL));
    }
    return restResponse.body();
  }

  private static RSAPrivateKey getPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
    String privateKeyPEM = key;
    privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
    privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
    privateKeyPEM = privateKeyPEM.replaceAll("\n", "");
    byte[] encoded = Base64.decodeBase64(privateKeyPEM);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
    RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
    return privKey;
  }

  private String generateTokenFromPrivateKey(GithubAppConfig githubAppConfig) throws Exception {
    Algorithm algorithm = Algorithm.RSA256(getPrivateKeyFromString(githubAppConfig.getPrivateKey()));
    return JWT.create()
        .withIssuer(githubAppConfig.getAppId())
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + EXP_TIME))
        .sign(algorithm);
  }

  private String getAuthToken(String authToken) {
    return format("Bearer %s", authToken);
  }
}

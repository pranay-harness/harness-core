package software.wings.helpers.ext.gcb;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.network.Http.getOkHttpClientBuilder;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.GcpConfig;
import software.wings.exception.GcbClientException;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.service.impl.GcpHelperService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class GcbServiceImpl implements GcbService {
  private static final String GCP_ERROR_MESSAGE = "Invalid Google Cloud Platform credentials.";
  private final GcpHelperService gcpHelperService;

  @Inject
  public GcbServiceImpl(GcpHelperService gcpHelperService) {
    this.gcpHelperService = gcpHelperService;
  }

  @Override
  public BuildOperationDetails createBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String projectId, GcbBuildDetails buildParams) {
    try {
      Response<BuildOperationDetails> response =
          getGcbRestClient()
              .createBuild(getBasicAuthHeader(gcpConfig, encryptionDetails), projectId, buildParams)
              .execute();
      return response.body();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public GcbBuildDetails getBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String projectId, String buildId) {
    try {
      Response<GcbBuildDetails> response =
          getGcbRestClient().getBuild(getBasicAuthHeader(gcpConfig, encryptionDetails), projectId, buildId).execute();
      return response.body();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public BuildOperationDetails runTrigger(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String projectId, String triggerId, RepoSource repoSource) {
    try {
      Response<BuildOperationDetails> response =
          getGcbRestClient()
              .runTrigger(getBasicAuthHeader(gcpConfig, encryptionDetails), projectId, triggerId, repoSource)
              .execute();
      return response.body();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  private GcbRestClient getGcbRestClient() {
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(5, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(GcbRestClient.baseUrl))
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(GcbRestClient.baseUrl)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(GcbRestClient.class);
  }

  private String getBasicAuthHeader(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    GoogleCredential gc = gcpHelperService.getGoogleCredential(gcpConfig, encryptionDetails);

    if (gc.refreshToken()) {
      return Credentials.basic("_token", gc.getAccessToken());
    } else {
      String msg = "Could not refresh token for google cloud provider";
      logger.warn(msg);
      throw new GcbClientException(msg);
    }
  }
}

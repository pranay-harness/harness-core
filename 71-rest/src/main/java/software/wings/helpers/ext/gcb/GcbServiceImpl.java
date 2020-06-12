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
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.GcpConfig;
import software.wings.exception.GcbClientException;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildTriggers;
import software.wings.helpers.ext.gcb.models.GcbTrigger;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.helpers.ext.gcs.GcsRestClient;
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
          getRestClient(GcbRestClient.class, GcbRestClient.baseUrl)
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
          getRestClient(GcbRestClient.class, GcbRestClient.baseUrl)
              .getBuild(getBasicAuthHeader(gcpConfig, encryptionDetails), projectId, buildId)
              .execute();
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
          getRestClient(GcbRestClient.class, GcbRestClient.baseUrl)
              .runTrigger(getBasicAuthHeader(gcpConfig, encryptionDetails), projectId, triggerId, repoSource)
              .execute();
      return response.body();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public String fetchBuildLogs(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String fileName) {
    try {
      final String bucket = bucketName.replace("gs://", "");
      Response<ResponseBody> response =
          getRestClient(GcsRestClient.class, GcsRestClient.baseUrl)
              .fetchLogs(getBasicAuthHeader(gcpConfig, encryptionDetails), bucket, fileName)
              .execute();
      return response.body().string();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  @Override
  public List<GcbTrigger> getAllTriggers(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String projectId) {
    try {
      Response<GcbBuildTriggers> response =
          getRestClient(GcbRestClient.class, GcbRestClient.baseUrl)
              .getAllTriggers(getBasicAuthHeader(gcpConfig, encryptionDetails), projectId)
              .execute();
      return response.body().getTriggers();
    } catch (IOException e) {
      throw new GcbClientException(GCP_ERROR_MESSAGE, e);
    }
  }

  protected <T> T getRestClient(final Class<T> client, String baseUrl) {
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(5, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(baseUrl))
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(baseUrl)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(client);
  }

  protected String getBasicAuthHeader(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    GoogleCredential gc = gcpHelperService.getGoogleCredential(gcpConfig, encryptionDetails);

    if (gc.refreshToken()) {
      return String.join(" ", "Bearer", gc.getAccessToken());
    } else {
      String msg = "Could not refresh token for google cloud provider";
      logger.warn(msg);
      throw new GcbClientException(msg);
    }
  }
}

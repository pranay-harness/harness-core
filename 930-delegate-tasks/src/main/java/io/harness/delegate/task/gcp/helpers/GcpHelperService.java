package io.harness.delegate.task.gcp.helpers;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.eraro.ErrorCode.INVALID_CLOUD_PROVIDER;
import static io.harness.exception.WingsException.USER;

import io.harness.delegate.task.artifacts.gcr.exceptions.GcbClientException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.gcp.helpers.GcpCredentialsHelperService;
import io.harness.gcp.helpers.GcpHttpTransportHelperService;
import io.harness.network.Http;
import io.harness.serializer.JsonUtils;

import software.wings.beans.TaskType;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.storage.Storage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by bzane on 2/22/17
 */
@Singleton
@Slf4j
public class GcpHelperService {
  public static final String LOCATION_DELIMITER = "/";
  public static final String ALL_LOCATIONS = "-";

  private static final int SLEEP_INTERVAL_SECS = 5;
  private static final int TIMEOUT_MINS = 30;
  private static final String GOOGLE_INTERNAL_COMPUTE_METADATA_DEFAULT_TOKEN_URL =
      "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token?scopes=";
  private static final String GOOGLE_META_BASE_URL = "http://metadata.google.internal/computeMetadata";

  @Inject private GcpHttpTransportHelperService gcpHttpTransportHelperService;
  @Inject private GcpCredentialsHelperService gcpCredentialsHelperService;

  /**
   * Gets a GCP container service.
   *
   * @param serviceAccountKeyFileContent
   * @param isUseDelegate
   *
   * @return the gke container service
   */
  public Container getGkeContainerService(char[] serviceAccountKeyFileContent, boolean isUseDelegate) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(serviceAccountKeyFileContent, isUseDelegate);
      return new Container.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  /**
   * Gets a GCS Service
   *
   * @param serviceAccountKeyFileContent
   * @param isUseDelegate
   *
   * @return the gcs storage service
   */
  public Storage getGcsStorageService(char[] serviceAccountKeyFileContent, boolean isUseDelegate) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(serviceAccountKeyFileContent, isUseDelegate);
      return new Storage.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Compute getGCEService(char[] serviceAccountKeyFileContent, String projectId, boolean isUseDelegate) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(serviceAccountKeyFileContent, isUseDelegate);
      return new Compute.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Monitoring getMonitoringService(char[] serviceAccountKeyFileContent, String projectId, boolean isUseDelegate) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(serviceAccountKeyFileContent, isUseDelegate);
      return new Monitoring.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Logging getLoggingResource(char[] serviceAccountKeyFileContent, String projectId, boolean isUseDelegate) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(serviceAccountKeyFileContent, isUseDelegate);
      return new Logging.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public GoogleCredential getGoogleCredential(char[] serviceAccountKeyFileContent, boolean isUseDelegate)
      throws IOException {
    if (isUseDelegate) {
      return gcpCredentialsHelperService.getApplicationDefaultCredentials();
    }
    validateServiceAccountKey(serviceAccountKeyFileContent);
    return checkIfUseProxyAndGetGoogleCredentials(serviceAccountKeyFileContent);
  }

  private GoogleCredential checkIfUseProxyAndGetGoogleCredentials(char[] serviceAccountKeyFileContent)
      throws IOException {
    String tokenUri =
        (String) (JsonUtils.asObject(new String(serviceAccountKeyFileContent), HashMap.class)).get("token_uri");
    return Http.getProxyHostName() != null && !Http.shouldUseNonProxy(tokenUri)
        ? gcpCredentialsHelperService.getGoogleCredentialWithProxyConfiguredHttpTransport(serviceAccountKeyFileContent)
        : gcpCredentialsHelperService.getGoogleCredentialWithDefaultHttpTransport(serviceAccountKeyFileContent);
  }

  private void validateServiceAccountKey(char[] serviceAccountKeyFileContent) {
    if (hasNone(serviceAccountKeyFileContent)) {
      throw new InvalidRequestException("Empty service key found. Unable to validate", USER);
    }
  }

  @SuppressWarnings("PMD")
  public String getDefaultCredentialsAccessToken(String taskTypeName) {
    OkHttpClient client = Http.getOkHttpClientBuilder()
                              .connectTimeout(10, TimeUnit.SECONDS)
                              .proxy(Http.checkAndGetNonProxyIfApplicable(GOOGLE_META_BASE_URL))
                              .build();
    Request request = new Request.Builder()
                          .header("Metadata-Flavor", "Google")
                          .url(GOOGLE_INTERNAL_COMPUTE_METADATA_DEFAULT_TOKEN_URL + ContainerScopes.CLOUD_PLATFORM)
                          .build();
    try {
      okhttp3.Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        log.info(taskTypeName + " - Fetched OAuth2 access token from metadata server");
        return String.join(
            " ", "Bearer", (String) JsonUtils.asObject(response.body().string(), HashMap.class).get("access_token"));
      }
      log.error(taskTypeName + " - Failed to fetch access token from metadata xxxxxxxx " + response);
      throw new GcbClientException("Failed to fetch access token from metadata server");
    } catch (IOException | NullPointerException e) {
      log.error(taskTypeName + " - Failed to get accessToken due to: ", e);
      throw new InvalidRequestException("Can not retrieve accessToken from from cluster meta");
    }
  }

  @SuppressWarnings("PMD")
  public String getClusterProjectId(String taskTypeName) {
    OkHttpClient client = Http.getOkHttpClientBuilder()
                              .connectTimeout(10, TimeUnit.SECONDS)
                              .proxy(Http.checkAndGetNonProxyIfApplicable(GOOGLE_META_BASE_URL))
                              .build();
    Request request = new Request.Builder()
                          .header("Metadata-Flavor", "Google")
                          .url("http://metadata.google.internal/computeMetadata/v1/project/project-id")
                          .build();
    try {
      okhttp3.Response response = client.newCall(request).execute();
      String projectId = response.body().string();
      log.info(taskTypeName + " - Fetched projectId from metadata xxxxxxxx " + projectId);
      return projectId;
    } catch (IOException | NullPointerException e) {
      throw new InvalidRequestException("Can not retrieve project-id from from cluster meta");
    }
  }

  public String getBasicAuthHeader(char[] serviceAccountKeyFileContent, boolean isUseDelegate) throws IOException {
    if (isUseDelegate) {
      return getDefaultCredentialsAccessToken(TaskType.GCP_TASK.name());
    }
    GoogleCredential gc = getGoogleCredential(serviceAccountKeyFileContent, isUseDelegate);
    try {
      if (gc.refreshToken()) {
        return Credentials.basic("_token", gc.getAccessToken());
      } else {
        String msg = "Could not refresh token for google cloud provider";
        log.warn(msg);
        throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE, USER).addParam("message", msg);
      }
    } catch (TokenResponseException e) {
      throw new InvalidRequestException("407 Proxy Authentication Required");
    }
  }

  /**
   * Gets sleep interval secs.
   *
   * @return the sleep interval secs
   */
  public int getSleepIntervalSecs() {
    return SLEEP_INTERVAL_SECS;
  }

  /**
   * Gets timeout mins.
   *
   * @return the timeout mins
   */
  public int getTimeoutMins() {
    return TIMEOUT_MINS;
  }
}

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_CLOUD_PROVIDER;
import static io.harness.exception.WingsException.USER;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.storage.Storage;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import software.wings.beans.GcpConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

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

  @Inject private EncryptionService encryptionService;

  /**
   * Validate credential.
   *
   */
  public void validateCredential(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    getGkeContainerService(gcpConfig, encryptedDataDetails);
  }

  /**
   * Gets a GCP container service.
   *
   * @return the gke container service
   */
  public Container getGkeContainerService(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails);
      return new Container.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      logger.error("Error getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  /**
   * Gets a GCS Service
   *
   * @return the gcs storage service
   */
  public Storage getGcsStorageService(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails);
      return new Storage.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      logger.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Compute getGCEService(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String projectId) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails);
      return new Compute.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      logger.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Monitoring getMonitoringService(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String projectId) throws IOException {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails);
      return new Monitoring.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      logger.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Logging getLoggingResource(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String projectId) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails);
      return new Logging.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      logger.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public GoogleCredential getGoogleCredential(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    if (isNotEmpty(encryptedDataDetails)) {
      encryptionService.decrypt(gcpConfig, encryptedDataDetails);
    }
    if (isEmpty(gcpConfig.getServiceAccountKeyFileContent())) {
      throw new InvalidRequestException("Empty service key found. Unable to validate", USER);
    }
    GoogleCredential credential = GoogleCredential.fromStream(
        IOUtils.toInputStream(String.valueOf(gcpConfig.getServiceAccountKeyFileContent()), Charset.defaultCharset()));
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
    }
    return credential;
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

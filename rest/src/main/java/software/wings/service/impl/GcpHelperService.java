package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
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
public class GcpHelperService {
  /**
   * The constant ZONE_DELIMITER.
   */
  public static final String ZONE_DELIMITER = "/";
  /**
   * The constant ALL_ZONES.
   */
  public static final String ALL_ZONES = "-";

  private static final int SLEEP_INTERVAL_SECS = 5;
  private static final int TIMEOUT_MINS = 30;

  private static final Logger logger = LoggerFactory.getLogger(GcpHelperService.class);

  @Inject private EncryptionService encryptionService;

  /**
   * Validate credential.
   *
   */
  public void validateCredential(GcpConfig gcpConfig) {
    getGkeContainerService(gcpConfig, Collections.emptyList());
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
      throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      logger.error("Error getting Google container service", e);
      throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public GoogleCredential getGoogleCredential(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    if (isNotEmpty(encryptedDataDetails)) {
      encryptionService.decrypt(gcpConfig, encryptedDataDetails);
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

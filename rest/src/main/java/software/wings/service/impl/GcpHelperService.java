package software.wings.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

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

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Validate credential.
   *
   * @param credentials the credentials
   */
  public void validateCredential(String credentials) {
    getGkeContainerService(credentials);
  }

  /**
   * Gets a GCP container service.
   *
   * @param credentials the credentials
   * @return the gke container service
   */
  public Container getGkeContainerService(String credentials) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = GoogleCredential.fromStream(IOUtils.toInputStream(credentials));
      if (credential.createScopedRequired()) {
        credential = credential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
      }
      return new Container.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google container service: {}", e.getMessage(), e);
      for (StackTraceElement elem : e.getStackTrace()) {
        logger.error("Trace: {}", elem.toString());
      }
      throw new WingsException(
          ErrorCode.INVALID_CLOUD_PROVIDER, "message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      logger.error("Error getting Google container service: {}", e.getMessage(), e);
      for (StackTraceElement elem : e.getStackTrace()) {
        logger.error("Trace: {}", elem.toString());
      }
      throw new WingsException(
          ErrorCode.INVALID_CLOUD_PROVIDER, "message", "Invalid Google Cloud Platform credentials.");
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

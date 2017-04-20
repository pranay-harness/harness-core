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
  public static final String ZONE_DELIMITER = "/";
  public static final String ALL_ZONES = "-";

  private static final int SLEEP_INTERVAL_SECS = 5;
  private static final int TIMEOUT_MINS = 30;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Gets a GCP container service.
   *
   */
  public Container getGkeContainerService(String credentials) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = GoogleCredential.fromStream(IOUtils.toInputStream(credentials));
      if (credential.createScopedRequired()) {
        credential = credential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
      }
      return new Container.Builder(transport, jsonFactory, credential).setApplicationName("Wings").build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google container service.", e);
      throw new WingsException(ErrorCode.INVALID_CREDENTIAL, "Invalid Google Cloud Platform credentials.", e);
    } catch (IOException e) {
      logger.error("Error getting Google container service.", e);
      throw new WingsException(ErrorCode.INVALID_CREDENTIAL, "Invalid Google Cloud Platform credentials.", e);
    }
  }

  public int getSleepIntervalSecs() {
    return SLEEP_INTERVAL_SECS;
  }

  public int getTimeoutMins() {
    return TIMEOUT_MINS;
  }
}

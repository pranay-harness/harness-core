package software.wings.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Created by bzane on 2/22/17
 */
@Singleton
public class GkeHelperService {
  private static final int SLEEP_INTERVAL_MS = 5 * 1000;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Gets a GKE container service.
   *
   */
  public Container getGkeContainerService(String appName) {
    GoogleCredential credential = null;
    try {
      credential = GoogleCredential.getApplicationDefault();
      if (credential.createScopedRequired()) {
        credential = credential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
      }
    } catch (IOException e) {
      logger.error("Error getting Google credential.", e);
    }

    Container containerService = null;
    try {
      containerService =
          new Container
              .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential)
              .setApplicationName(appName)
              .build();
    } catch (GeneralSecurityException e) {
      logger.error("Security exception getting Google container service.", e);
    } catch (IOException e) {
      logger.error("Error getting Google container service.", e);
    }
    return containerService;
  }

  public int getSleepIntervalMs() {
    return SLEEP_INTERVAL_MS;
  }
}

package software.wings.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bzane on 2/22/17.
 */
@Singleton
public class KubernetesHelperService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Map<String, KubernetesClient> clientCacheMap = new HashMap<>();

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
      e.printStackTrace();
    }

    Container containerService = null;
    try {
      containerService =
          new Container
              .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential)
              .setApplicationName(appName)
              .build();
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return containerService;
  }

  /**
   * Gets a Kubernetes client.
   */
  public KubernetesClient getKubernetesClient(SettingAttribute settingAttribute) {
    KubernetesConfig kubernetesConfig = (KubernetesConfig) settingAttribute.getValue();
    KubernetesClient clientCached = null;
    String masterUrl = kubernetesConfig.getApiServerUrl();
    if (clientCacheMap.containsKey(masterUrl)) {
      Config config = clientCacheMap.get(masterUrl).getConfiguration();
      if (kubernetesConfig.getUsername().equals(config.getUsername())
          && kubernetesConfig.getPassword().equals(config.getPassword())) {
        clientCached = clientCacheMap.get(masterUrl);
      }
    }
    if (clientCached == null) {
      clientCached = new DefaultKubernetesClient(new ConfigBuilder()
                                                     .withMasterUrl(masterUrl)
                                                     .withTrustCerts(true)
                                                     .withUsername(kubernetesConfig.getUsername())
                                                     .withPassword(kubernetesConfig.getPassword())
                                                     .withNamespace("default")
                                                     .build());
      logger.info("Connected to cluster " + masterUrl);
      clientCacheMap.put(masterUrl, clientCached);
    }
    return clientCached;
  }
}

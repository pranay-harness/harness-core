package software.wings.service.impl;

import com.google.inject.Singleton;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;

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
   * Gets a Kubernetes client.
   */
  public KubernetesClient getKubernetesClient(SettingAttribute settingAttribute) {
    KubernetesConfig kubernetesConfig = (KubernetesConfig) settingAttribute.getValue();
    KubernetesClient clientCached = null;
    String masterUrl = kubernetesConfig.getMasterUrl();
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
      logger.info("Connected to cluster {}", masterUrl);
      clientCacheMap.put(masterUrl, clientCached);
    }
    return clientCached;
  }
}

package software.wings.service.impl;

import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brett on 2/22/17
 */
@Singleton
public class KubernetesHelperService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Map<String, KubernetesClient> clientCacheMap = new HashMap<>();

  /**
   * Gets a Kubernetes client.
   */
  public KubernetesClient getKubernetesClient(KubernetesConfig kubernetesConfig) {
    KubernetesClient clientCached = null;
    String masterUrl = kubernetesConfig.getMasterUrl();
    if (clientCacheMap.containsKey(masterUrl)) {
      Config config = clientCacheMap.get(masterUrl).getConfiguration();
      if (kubernetesConfig.getUsername().equals(config.getUsername())
          && Arrays.equals(kubernetesConfig.getPassword(), config.getPassword().toCharArray())) {
        clientCached = clientCacheMap.get(masterUrl);
      }
    }
    if (clientCached == null) {
      clientCached = new DefaultKubernetesClient(new ConfigBuilder()
                                                     .withMasterUrl(masterUrl)
                                                     .withTrustCerts(true)
                                                     .withUsername(kubernetesConfig.getUsername())
                                                     .withPassword(new String(kubernetesConfig.getPassword()))
                                                     .withNamespace("harness")
                                                     .build());
      logger.info("Connected to cluster {}", masterUrl);
      clientCacheMap.put(masterUrl, clientCached);
    }
    return clientCached;
  }

  public void validateCredential(KubernetesConfig kubernetesConfig) {
    getKubernetesClient(kubernetesConfig).replicationControllers().list();
  }
}

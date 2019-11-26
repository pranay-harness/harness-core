package io.harness.perpetualtask.k8s.metrics.client;

import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.harness.perpetualtask.k8s.metrics.client.impl.DefaultK8sMetricsClient;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class K8sMetricsExtensionAdapter implements ExtensionAdapter<K8sMetricsClient> {
  private static final ConcurrentMap<String, Boolean> cache = new ConcurrentHashMap<>();

  @Override
  public Class<K8sMetricsClient> getExtensionType() {
    return K8sMetricsClient.class;
  }

  @Override
  public Boolean isAdaptable(Client client) {
    return cache.computeIfAbsent(client.getMasterUrl().toString(), uri -> {
      final RootPaths rootPaths = client.rootPaths();
      if (rootPaths != null) {
        final List<String> paths = rootPaths.getPaths();
        if (paths != null) {
          for (String path : paths) {
            if (path.endsWith("metrics.k8s.io") || path.contains("metrics.k8s.io/")) {
              return true;
            }
          }
        }
      }
      return false;
    });
  }

  @Override
  public K8sMetricsClient adapt(Client client) {
    return new DefaultK8sMetricsClient(client.adapt(OkHttpClient.class), client.getConfiguration());
  }
}

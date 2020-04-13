package io.harness.perpetualtask.k8s.metrics.client;

import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.harness.perpetualtask.k8s.metrics.client.impl.DefaultK8sMetricsClient;
import okhttp3.OkHttpClient;

public class K8sMetricsExtensionAdapter implements ExtensionAdapter<K8sMetricsClient> {
  @Override
  public Class<K8sMetricsClient> getExtensionType() {
    return K8sMetricsClient.class;
  }

  @Override
  public Boolean isAdaptable(Client client) {
    // not checking if the resource is present as that requires "get /" permissions on the cluster.
    return Boolean.TRUE;
  }

  @Override
  public K8sMetricsClient adapt(Client client) {
    return new DefaultK8sMetricsClient(client.adapt(OkHttpClient.class), client.getConfiguration());
  }
}

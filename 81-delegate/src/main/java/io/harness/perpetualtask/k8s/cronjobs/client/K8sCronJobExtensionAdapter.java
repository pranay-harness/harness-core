package io.harness.perpetualtask.k8s.cronjobs.client;

import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.harness.perpetualtask.k8s.cronjobs.client.impl.DefaultK8sCronJobClient;
import okhttp3.OkHttpClient;

public class K8sCronJobExtensionAdapter implements ExtensionAdapter<K8sCronJobClient> {
  @Override
  public Class<K8sCronJobClient> getExtensionType() {
    return K8sCronJobClient.class;
  }

  @Override
  public Boolean isAdaptable(Client client) {
    return Boolean.TRUE;
  }

  @Override
  public K8sCronJobClient adapt(Client client) {
    return new DefaultK8sCronJobClient(client.adapt(OkHttpClient.class), client.getConfiguration());
  }
}

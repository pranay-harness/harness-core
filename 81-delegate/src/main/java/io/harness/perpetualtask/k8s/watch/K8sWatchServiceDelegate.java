package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.event.client.EventPublisher;
import io.harness.serializer.KryoUtils;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class K8sWatchServiceDelegate {
  private Map<String, Watcher> watchMap; // <id, Watch>
  @Inject private KubernetesClientFactory kubernetesClientFactory;
  @Inject private EventPublisher eventPublisher;

  public K8sWatchServiceDelegate() {
    this.watchMap = new ConcurrentHashMap<>();
  }

  public List<String> list() {
    return new ArrayList<>(watchMap.keySet());
  }

  public String create(K8sWatchTaskParams params) {
    // TODO: first check if two watch requests are the same
    K8sClusterConfig k8sClusterConfig =
        (K8sClusterConfig) KryoUtils.asObject(params.getK8SClusterConfig().toByteArray());
    KubernetesClient client = kubernetesClientFactory.newKubernetesClient(k8sClusterConfig);

    Watcher watcher = null;
    String kind = params.getK8SResourceKind();
    switch (kind) {
      case "Pod":
        watcher = new PodWatcher(client, eventPublisher);
        break;
      case "Node":
        watcher = new NodeWatcher(client, eventPublisher);
        break;
      default:
        break;
    }

    String id = UUID.randomUUID().toString(); // generate a unique id
    watchMap.putIfAbsent(id, watcher);

    return id;
  }

  public void delete(String id) {
    Watcher watcher = watchMap.get(id);
    watcher.onClose(new KubernetesClientException("STOP"));
    watchMap.remove(id);
  }
}

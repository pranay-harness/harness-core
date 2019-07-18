package software.wings.delegatetasks.k8s.watch;

import static java.util.Collections.emptyList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import software.wings.beans.KubernetesConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.impl.KubernetesHelperService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class K8sWatchService {
  private Map<String, Watcher> watchMap; // <id, Watch>
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  public K8sWatchService() {
    this.watchMap = new ConcurrentHashMap<>();
  }

  public List<String> list() {
    return new ArrayList(watchMap.keySet());
  }

  public String register(WatchRequest watchRequest) {
    // TODO: first check if two watch requests are the same

    Watcher watcher = null;
    String kind = watchRequest.getK8sResourceKind();

    switch (kind) {
      case "Pod":
        watcher = new PodWatcher(getKubernetesClient(watchRequest.getK8sClusterConfig()), watchRequest.getNamespace());
        break;
      default:
        break;
    }

    // generate a unique id
    String id = UUID.randomUUID().toString();
    watchMap.putIfAbsent(id, watcher);

    return id;
  }

  public void remove(String id) {
    Watcher watcher = watchMap.get(id);
    watcher.onClose(new KubernetesClientException("STOP"));
    watchMap.remove(id);
  }

  private KubernetesClient getKubernetesClient(K8sClusterConfig k8sClusterConfig) {
    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig);
    KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, emptyList());
  }
}

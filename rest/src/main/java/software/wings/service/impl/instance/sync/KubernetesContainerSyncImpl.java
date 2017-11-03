package software.wings.service.impl.instance.sync;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo.Builder;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.request.KubernetesFilter;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by brett on 9/6/17
 */
public class KubernetesContainerSyncImpl implements ContainerSync {
  @Inject private KubernetesContainerService kubernetesContainerService;

  @Override
  public ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest, String workflowId, String appId) {
    KubernetesFilter filter = (KubernetesFilter) syncRequest.getFilter();
    List<ContainerInfo> result = new ArrayList<>();
    Set<String> replicationControllerNameSet = filter.getReplicationControllerNameSet();

    for (String replicationControllerName : replicationControllerNameSet) {
      ReplicationController replicationController = kubernetesContainerService.getController(
          filter.getKubernetesConfig(), Collections.emptyList(), replicationControllerName);
      if (replicationController != null) {
        Map<String, String> labels = replicationController.getMetadata().getLabels();
        List<Service> services =
            kubernetesContainerService.getServices(filter.getKubernetesConfig(), Collections.emptyList(), labels)
                .getItems();
        String serviceName = services.size() > 0 ? services.get(0).getMetadata().getName() : "None";
        for (Pod pod : kubernetesContainerService.getPods(filter.getKubernetesConfig(), Collections.emptyList(), labels)
                           .getItems()) {
          if (pod.getStatus().getPhase().equals("Running")) {
            List<ReplicationController> rcs = kubernetesContainerService
                                                  .getControllers(filter.getKubernetesConfig(), Collections.emptyList(),
                                                      pod.getMetadata().getLabels())
                                                  .getItems();
            String rcName = rcs.size() > 0 ? rcs.get(0).getMetadata().getName() : "None";

            KubernetesContainerInfo kubernetesContainerInfo = Builder.aKubernetesContainerInfo()
                                                                  .withClusterName(filter.getClusterName())
                                                                  .withPodName(pod.getMetadata().getName())
                                                                  .withReplicationControllerName(rcName)
                                                                  .withServiceName(serviceName)
                                                                  .build();
            result.add(kubernetesContainerInfo);
          }
        }
      }
    }
    return ContainerSyncResponse.builder().containerInfoList(result).build();
  }
}

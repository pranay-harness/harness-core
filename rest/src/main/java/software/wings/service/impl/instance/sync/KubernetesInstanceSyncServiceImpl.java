package software.wings.service.impl.instance.sync;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo.Builder;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.service.impl.instance.sync.request.InstanceSyncRequest;
import software.wings.service.impl.instance.sync.request.KubernetesFilter;
import software.wings.service.impl.instance.sync.response.InstanceSyncResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 9/6/17
 */
public class KubernetesInstanceSyncServiceImpl implements InstanceSyncService {
  @Inject private KubernetesContainerService kubernetesContainerService;

  @Override
  public InstanceSyncResponse getInstances(InstanceSyncRequest syncRequest) {
    KubernetesFilter filter = (KubernetesFilter) syncRequest.getFilter();
    List<ContainerInfo> result = new ArrayList<>();
    List<String> replicationControllerNameList = filter.getReplicationControllerNameList();
    for (String replicationControllerName : replicationControllerNameList) {
      Map<String, String> labels =
          kubernetesContainerService.getController(filter.getKubernetesConfig(), replicationControllerName)
              .getMetadata()
              .getLabels();
      //      labels.remove("revision");
      List<Service> services = kubernetesContainerService.getServices(filter.getKubernetesConfig(), labels).getItems();
      String serviceName = services.size() > 0 ? services.get(0).getMetadata().getName() : "None";
      for (Pod pod : kubernetesContainerService.getPods(filter.getKubernetesConfig(), labels).getItems()) {
        if (pod.getStatus().getPhase().equals("Running")) {
          List<ReplicationController> rcs =
              kubernetesContainerService.getControllers(filter.getKubernetesConfig(), pod.getMetadata().getLabels())
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
    return InstanceSyncResponse.Builder.anInstanceSyncResponse().withContainerInfoList(result).build();
  }
}

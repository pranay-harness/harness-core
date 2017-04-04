package software.wings.cloudprovider.gke;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.service.impl.KubernetesHelperService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by brett on 2/9/17
 */
@Singleton
public class KubernetesContainerServiceImpl implements KubernetesContainerService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();

  @Override
  public ReplicationController createController(KubernetesConfig kubernetesConfig, ReplicationController definition) {
    logger.info("Creating controller {}", definition.getMetadata().getName());
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig)
        .replicationControllers()
        .createOrReplace(definition);
  }

  @Override
  public ReplicationController getController(KubernetesConfig kubernetesConfig, String name) {
    return name != null
        ? kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().withName(name).get()
        : null;
  }

  @Override
  public ReplicationControllerList listControllers(KubernetesConfig kubernetesConfig) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().list();
  }

  @Override
  public void deleteController(KubernetesConfig kubernetesConfig, String name) {
    logger.info("Deleting controller {}", name);
    kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().withName(name).delete();
  }

  @Override
  public void setControllerPodCount(KubernetesConfig kubernetesConfig, String name, int number) {
    kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().withName(name).scale(number);
    logger.info("Scaled controller {} to {} instances", name, number);
  }

  @Override
  public int getControllerPodCount(KubernetesConfig kubernetesConfig, String name) {
    return getController(kubernetesConfig, name).getSpec().getReplicas();
  }

  @Override
  public List<String> getPodNames(KubernetesConfig kubernetesConfig, String replicationControllerName) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig)
        .pods()
        .withLabels(getController(kubernetesConfig, replicationControllerName).getMetadata().getLabels())
        .list()
        .getItems()
        .stream()
        .map(pod -> pod.getMetadata().getName())
        .collect(Collectors.toList());
  }

  @Override
  public Service createService(KubernetesConfig kubernetesConfig, Service definition) {
    logger.info("Creating service {}", definition.getMetadata().getName());
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().createOrReplace(definition);
  }

  @Override
  public Service getService(KubernetesConfig kubernetesConfig, String name) {
    return name != null ? kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().withName(name).get()
                        : null;
  }

  @Override
  public ServiceList listServices(KubernetesConfig kubernetesConfig) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().list();
  }

  @Override
  public void deleteService(KubernetesConfig kubernetesConfig, String name) {
    ClientResource<Service, DoneableService> service =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().withName(name);
    service.delete();
    logger.info("Deleted service {}", name);
  }

  public void checkStatus(KubernetesConfig kubernetesConfig, String rcName, String serviceName) {
    KubernetesClient client = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    String masterUrl = client.getMasterUrl().toString();
    ReplicationController rc = client.replicationControllers().inNamespace("default").withName(rcName).get();
    if (rc != null) {
      String rcLink = masterUrl + rc.getMetadata().getSelfLink().substring(1);
      logger.info("Replication controller {}: {}", rcName, rcLink);
    } else {
      logger.info("Replication controller {} does not exist", rcName);
    }
    Service service = client.services().withName(serviceName).get();
    if (service != null) {
      String serviceLink = masterUrl + service.getMetadata().getSelfLink().substring(1);
      logger.info("Service %s: {}", serviceName, serviceLink);
    } else {
      logger.info("Service {} does not exist", serviceName);
    }
  }

  public void cleanup(KubernetesConfig kubernetesConfig) {
    KubernetesClient client = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    if (client.services().list().getItems() != null) {
      client.services().delete();
      logger.info("Deleted existing services");
    }
    if (client.replicationControllers().list().getItems() != null) {
      client.replicationControllers().delete();
      logger.info("Deleted existing replication controllers");
    }
  }
}

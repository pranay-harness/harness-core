package software.wings.cloudprovider.gke;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.PodSpecFluent;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerFluent;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpecFluent;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceFluent;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.service.impl.KubernetesHelperService;

import java.util.Map;

/**
 * Created by brett on 2/9/17
 */
@Singleton
public class KubernetesContainerServiceImpl implements KubernetesContainerService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();

  @Override
  public ReplicationController createController(KubernetesConfig kubernetesConfig, Map<String, String> params) {
    ResourceRequirements resourceRequirements = new ResourceRequirements();
    resourceRequirements.setRequests(
        ImmutableMap.of("cpu", new Quantity(params.get("cpu")), "memory", new Quantity(params.get("memory"))));

    PodSpecFluent.ContainersNested<PodTemplateSpecFluent.SpecNested<ReplicationControllerSpecFluent.TemplateNested<
        ReplicationControllerFluent.SpecNested<ReplicationControllerBuilder>>>> container =
        new ReplicationControllerBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName(params.get("name"))
            .addToLabels("app", params.get("appName"))
            .addToLabels("tier", params.get("tier"))
            .endMetadata()
            .withNewSpec()
            .withReplicas(Integer.valueOf(params.get("count")))
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", params.get("appName"))
            .addToLabels("tier", params.get("tier"))
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName(params.get("containerName"))
            .withImage(params.get("containerImage"))
            .withResources(resourceRequirements);
    if ("backend".equals(params.get("tier"))) {
      container.withArgs(params.get("port"));
    } else {
      container.addNewEnv().withName("GET_HOSTS_FROM").withValue("dns").endEnv();
    }
    ReplicationController rc = container.addNewPort()
                                   .withContainerPort(Integer.valueOf(params.get("port")))
                                   .endPort()
                                   .endContainer()
                                   .endSpec()
                                   .endTemplate()
                                   .endSpec()
                                   .build();

    rc = kubernetesHelperService.getKubernetesClient(kubernetesConfig)
             .replicationControllers()
             .inNamespace("default")
             .createOrReplace(rc);
    logger.info("Created {} controller {} for {}", params.get("tier"), params.get("name"), params.get("appName"));
    return rc;
  }

  @Override
  public void deleteController(KubernetesConfig kubernetesConfig, String name) {
    kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().withName(name).delete();
    logger.info("Deleted controller {}", name);
  }

  @Override
  public Service createService(KubernetesConfig kubernetesConfig, Map<String, String> params) {
    ServiceFluent.SpecNested<DoneableService> spec = kubernetesHelperService.getKubernetesClient(kubernetesConfig)
                                                         .services()
                                                         .createOrReplaceWithNew()
                                                         .withApiVersion("v1")
                                                         .withNewMetadata()
                                                         .withName(params.get("name"))
                                                         .addToLabels("app", params.get("appName"))
                                                         .addToLabels("tier", params.get("tier"))
                                                         .endMetadata()
                                                         .withNewSpec();
    if (params.containsKey("type")) {
      spec.withType(params.get("type"));
    }
    Service service = spec.addNewPort()
                          .withPort(Integer.valueOf(params.get("port")))
                          .withNewTargetPort()
                          .withIntVal(Integer.valueOf(params.get("targetPort")))
                          .endTargetPort()
                          .endPort()
                          .addToSelector("app", params.get("appName"))
                          .addToSelector("tier", params.get("tier"))
                          .endSpec()
                          .done();
    logger.info("Created {} service {} for {}", params.get("tier"), params.get("name"), params.get("appName"));
    return service;
  }

  @Override
  public void deleteService(KubernetesConfig kubernetesConfig, String name) {
    ClientResource<Service, DoneableService> service =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().withName(name);
    service.delete();
    logger.info("Deleted service {}", name);
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
  public ReplicationController getController(KubernetesConfig kubernetesConfig, String name) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().withName(name).get();
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

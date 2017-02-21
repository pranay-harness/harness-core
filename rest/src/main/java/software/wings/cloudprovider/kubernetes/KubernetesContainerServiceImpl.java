package software.wings.cloudprovider.kubernetes;

import com.google.common.collect.ImmutableMap;
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
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;

import java.util.Map;

/**
 * Created by brett on 2/9/17.
 */
@Singleton
public class KubernetesContainerServiceImpl implements KubernetesContainerService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private KubernetesClient clientCached;

  private KubernetesClient getClient(KubernetesConfig config) {
    if (clientCached == null || !config.getApiServerUrl().equals(clientCached.getMasterUrl().toString())
        || !config.getUsername().equals(clientCached.getConfiguration().getUsername())
        || !config.getPassword().equals(clientCached.getConfiguration().getPassword())) {
      clientCached = new DefaultKubernetesClient(new ConfigBuilder()
                                                     .withMasterUrl(config.getApiServerUrl())
                                                     .withTrustCerts(true)
                                                     .withUsername(config.getUsername())
                                                     .withPassword(config.getPassword())
                                                     .withNamespace("default")
                                                     .build());
      logger.info("Connected to cluster " + config.getApiServerUrl());
    }
    return clientCached;
  }

  @Override
  public void createController(KubernetesConfig config, Map<String, String> params) {
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

    getClient(config).replicationControllers().inNamespace("default").createOrReplace(rc);
    logger.info(
        "Created " + params.get("tier") + " controller " + params.get("name") + " for " + params.get("appName"));
  }

  @Override
  public void deleteController(KubernetesConfig config, Map<String, String> params) {}

  @Override
  public void createService(KubernetesConfig config, Map<String, String> params) {
    ServiceFluent.SpecNested<DoneableService> spec = getClient(config)
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
    spec.addNewPort()
        .withPort(80)
        .withNewTargetPort()
        .withIntVal(8080)
        .endTargetPort()
        .endPort()
        .addToSelector("app", params.get("appName"))
        .addToSelector("tier", params.get("tier"))
        .endSpec()
        .done();
    logger.info("Created " + params.get("tier") + " service " + params.get("name") + " for " + params.get("appName"));
  }

  @Override
  public void deleteService(KubernetesConfig config, Map<String, String> params) {}

  @Override
  public void setControllerPodCount(KubernetesConfig config, String name, int number) {
    getClient(config).replicationControllers().withName(name).scale(number);
    logger.info("Scaled controller " + name + " to " + number + " instances");
  }

  @Override
  public int getControllerPodCount(KubernetesConfig config, String name) {
    return 0;
  }

  public void checkStatus(KubernetesConfig config, String rcName, String serviceName) {
    KubernetesClient client = getClient(config);
    ReplicationController rc = client.replicationControllers().inNamespace("default").withName(rcName).get();
    logger.info("Replication controller " + rcName + ": " + client.getMasterUrl()
        + rc.getMetadata().getSelfLink().substring(1));
    Service service = client.services().withName(serviceName).get();
    logger.info(
        "Service " + serviceName + ": " + client.getMasterUrl() + service.getMetadata().getSelfLink().substring(1));
  }

  public void cleanup(KubernetesConfig config) {
    KubernetesClient client = getClient(config);
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

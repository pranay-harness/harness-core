package software.wings.cloudprovider.kubernetes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;

import java.util.Map;

/**
 * Created by brett on 2/9/17.
 */
@Singleton
public class KubernetesServiceImpl implements KubernetesService {
  private static final int SLEEP_INTERVAL = 5 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private KubernetesClient client;
  private Service service;

  public void createCluster(String masterUrl, Map<String, String> params) throws InterruptedException {
    client = new DefaultKubernetesClient(new ConfigBuilder()
                                             .withMasterUrl(masterUrl)
                                             .withTrustCerts(true)
                                             .withUsername(params.get("username"))
                                             .withPassword(params.get("password"))
                                             .withNamespace("default")
                                             .build());
  }

  public void destroyCluster() throws InterruptedException {
    client.services().delete();
    client.replicationControllers().delete();
  }

  @Override
  public void provisionNodes(SettingAttribute connectorConfig, String autoScalingGroupName, Integer clusterSize) {
    KubernetesConfig kubernetesConfig = (KubernetesConfig) connectorConfig.getValue();
    Config config = new ConfigBuilder()
                        .withMasterUrl(kubernetesConfig.getApiServerUrl())
                        .withUsername(kubernetesConfig.getUsername())
                        .withPassword(kubernetesConfig.getPassword())
                        .build();

    client = new DefaultKubernetesClient(config);
    for (int i = 0; i < clusterSize; i++) {
      client.nodes().createNew().done();
    }
  }

  @Override
  public void provisionNodes(
      SettingAttribute connectorConfig, Integer clusterSize, String launchConfigName, Map<String, Object> params) {
    KubernetesConfig kubernetesConfig = (KubernetesConfig) connectorConfig.getValue();
    Config config = new ConfigBuilder()
                        .withMasterUrl(kubernetesConfig.getApiServerUrl())
                        .withUsername(kubernetesConfig.getUsername())
                        .withPassword(kubernetesConfig.getPassword())
                        .build();

    client = new DefaultKubernetesClient(config);
    for (int i = 0; i < clusterSize; i++) {
      client.nodes().createNew().done();
    }
  }

  @Override
  public String deployService(SettingAttribute connectorConfig, String serviceDefinition) {
    return null;
  }

  @Override
  public void deleteService(SettingAttribute connectorConfig, String clusterName, String serviceName) {}

  @Override
  public void createFrontendService(Map<String, String> params) {
    client.services()
        .createOrReplaceWithNew()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(params.get("name"))
        .addToLabels("app", params.get("appName"))
        .addToLabels("tier", "frontend")
        .endMetadata()
        .withNewSpec()
        .withType(params.get("type"))
        .addNewPort()
        .withPort(80)
        .withNewTargetPort()
        .withIntVal(8080)
        .endTargetPort()
        .endPort()
        .addToSelector("app", params.get("appName"))
        .addToSelector("tier", "frontend")
        .endSpec()
        .done();
  }

  @Override
  public void createBackendService(Map<String, String> params) {
    client.services()
        .createOrReplaceWithNew()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(params.get("name"))
        .addToLabels("app", params.get("appName"))
        .addToLabels("tier", "backend")
        .endMetadata()
        .withNewSpec()
        .addNewPort()
        .withPort(80)
        .withNewTargetPort()
        .withIntVal(8080)
        .endTargetPort()
        .endPort()
        .addToSelector("app", params.get("appName"))
        .addToSelector("tier", "backend")
        .endSpec()
        .done();
  }

  @Override
  public void createFrontendController(Map<String, Quantity> requests, Map<String, String> params) {
    ResourceRequirements resourceRequirements = new ResourceRequirements();
    resourceRequirements.setRequests(requests);

    ReplicationController rc = new ReplicationControllerBuilder()
                                   .withApiVersion("v1")
                                   .withNewMetadata()
                                   .withName(params.get("name"))
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "frontend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .withReplicas(Integer.valueOf(params.get("count")))
                                   .withNewTemplate()
                                   .withNewMetadata()
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "frontend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .addNewContainer()
                                   .withName("webapp")
                                   .withImage(params.get("webappImage"))
                                   .withResources(resourceRequirements)
                                   .addNewEnv()
                                   .withName("GET_HOSTS_FROM")
                                   .withValue("dns")
                                   .endEnv()
                                   .addNewPort()
                                   .withContainerPort(8080)
                                   .endPort()
                                   .endContainer()
                                   .endSpec()
                                   .endTemplate()
                                   .endSpec()
                                   .build();

    client.replicationControllers().inNamespace("default").createOrReplace(rc);
  }

  @Override
  public void scaleFrontendController(String name, int number) {
    client.replicationControllers().withName(name).scale(number);
  }

  @Override
  public void createBackendController(Map<String, String> params) {
    ResourceRequirements resourceRequirements = new ResourceRequirements();
    resourceRequirements.setRequests(
        ImmutableMap.of("cpu", new Quantity(params.get("cpu")), "memory", new Quantity(params.get("memory"))));

    ReplicationController rc = new ReplicationControllerBuilder()
                                   .withApiVersion("v1")
                                   .withNewMetadata()
                                   .withName(params.get("name"))
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "backend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .withReplicas(Integer.valueOf(params.get("count")))
                                   .withNewTemplate()
                                   .withNewMetadata()
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "backend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .addNewContainer()
                                   .withName("server")
                                   .withImage(params.get("serverImage"))
                                   .withArgs(params.get("port"))
                                   .withResources(resourceRequirements)
                                   .addNewPort()
                                   .withContainerPort(8080)
                                   .endPort()
                                   .endContainer()
                                   .endSpec()
                                   .endTemplate()
                                   .endSpec()
                                   .build();

    client.replicationControllers().inNamespace("default").createOrReplace(rc);
  }

  private void checkStatus(String rcName, String serviceName) {
    ReplicationController rc = client.replicationControllers().inNamespace("default").withName(rcName).get();
    System.out.println("rc = " + rc);
    Service service = client.services().withName(serviceName).get();
    System.out.println("service = " + service);
  }

  private void cleanup() {
    client.services().delete();
    if (client.replicationControllers().list().getItems() != null) {
      client.replicationControllers().delete();
    }
  }

  public static void main(String... args) throws InterruptedException {
    KubernetesServiceImpl kubernetesService = new KubernetesServiceImpl();
    kubernetesService.createCluster(
        "https://35.184.26.158", ImmutableMap.of("username", "admin", "password", "1rcbvCsbiTxIjw88"));

    kubernetesService.cleanup();

    kubernetesService.createBackendController(ImmutableMap.of("name", "backend-ctrl", "appName", "testApp",
        "serverImage", "gcr.io/google-samples/node-hello", "port", "8080", "count", "2"));

    kubernetesService.createBackendService(ImmutableMap.of("name", "backend-service", "appName", "testApp"));

    kubernetesService.createFrontendController(
        ImmutableMap.of("cpu", new Quantity("100m"), "memory", new Quantity("100Mi")),
        ImmutableMap.of("name", "frontend-ctrl", "appName", "testApp", "webappImage",
            "gcr.io/google-samples/node-hello", "port", "8080", "count", "2"));

    kubernetesService.createFrontendService(
        ImmutableMap.of("name", "frontend-service", "appName", "testApp", "type", "LoadBalancer"));

    kubernetesService.scaleFrontendController("frontend-ctrl", 5);

    kubernetesService.checkStatus("backend-ctrl", "backend-service");
    kubernetesService.checkStatus("frontend-ctrl", "frontend-service");
  }
}

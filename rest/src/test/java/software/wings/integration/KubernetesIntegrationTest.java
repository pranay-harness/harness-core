package software.wings.integration;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.GcpHelperService.LOCATION_DELIMITER;

import com.google.api.services.container.model.NodePoolAutoscaling;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.GkeClusterServiceImpl;
import software.wings.cloudprovider.gke.KubernetesContainerServiceImpl;
import software.wings.rules.Integration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Integration
@Ignore
public class KubernetesIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesIntegrationTest.class);

  private static final SettingAttribute COMPUTE_PROVIDER_SETTING =
      aSettingAttribute()
          .withUuid("GCP_ID")
          .withValue(GcpConfig.builder().serviceAccountKeyFileContent("GCP_CONFIG_CONTENT".toCharArray()).build())
          .build();

  private static final String ZONE_CLUSTER = "us-central1-a" + LOCATION_DELIMITER + "brett-test";
  private static final String NAMESPACE = "default";

  public static void main(String[] args) throws InterruptedException {
    GkeClusterServiceImpl gkeClusterService = new GkeClusterServiceImpl();
    KubernetesContainerServiceImpl kubernetesService = new KubernetesContainerServiceImpl();

    List<String> clusters = gkeClusterService.listClusters(COMPUTE_PROVIDER_SETTING, Collections.emptyList());
    logger.info("Available clusters: {}", clusters);

    //    KubernetesConfig config = gkeClusterService.createCluster(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER,
    //        ImmutableMap.<String, String>builder()
    //            .put("nodeCount", "1")
    //            .put("machineType", "n1-highcpu-4")
    //            .put("masterUser", "master")
    //            .put("masterPwd", "foo!!bar$$")
    //            .build());

    KubernetesConfig config =
        gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, NAMESPACE);

    //    gkeClusterService.setNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null, true, 4, 8);
    //    gkeClusterService.setNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null, false, 4, 8);

    NodePoolAutoscaling autoscaling =
        gkeClusterService.getNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null);
    logger.info("Autoscale setting: {}", autoscaling);

    //    kubernetesService.cleanup(config);

    kubernetesService.createOrReplaceController(config, Collections.emptyList(),
        new ReplicationControllerBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName("backend-ctrl")
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .withReplicas(0)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("server")
            .withImage("gcr.io/exploration-161417/todolist")
            .withArgs("8080")
            .withNewResources()
            .withRequests(ImmutableMap.of("cpu", new Quantity("10m"), "memory", new Quantity("10Mi")))
            .withLimits(ImmutableMap.of("cpu", new Quantity("100m"), "memory", new Quantity("100Mi")))
            .endResources()
            .addNewPort()
            .withContainerPort(8080)
            .endPort()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());

    kubernetesService.createOrReplaceService(config, Collections.emptyList(),
        new ServiceBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName("backend-service")
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .addNewPort()
            .withPort(80)
            .withNewTargetPort()
            .withIntVal(8080)
            .endTargetPort()
            .endPort()
            .addToSelector("app", "testApp")
            .addToSelector("tier", "backend")
            .endSpec()
            .build());

    kubernetesService.createOrReplaceController(config, Collections.emptyList(),
        new ReplicationControllerBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName("frontend-ctrl")
            .addToLabels("app", "testApp")
            .addToLabels("tier", "frontend")
            .endMetadata()
            .withNewSpec()
            .withReplicas(0)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", "testApp")
            .addToLabels("tier", "frontend")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("webapp")
            .withImage("gcr.io/exploration-161417/todolist:latest")
            .addNewEnv()
            .withName("GET_HOSTS_FROM")
            .withValue("dns")
            .endEnv()
            .withNewResources()
            .withLimits(ImmutableMap.of("cpu", new Quantity("100m"), "memory", new Quantity("100Mi")))
            .endResources()
            .addNewPort()
            .withContainerPort(8080)
            .endPort()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());

    kubernetesService.createOrReplaceService(config, Collections.emptyList(),
        new ServiceBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName("frontend-service")
            .addToLabels("app", "testApp")
            .addToLabels("tier", "frontend")
            .endMetadata()
            .withNewSpec()
            .addNewPort()
            .withPort(80)
            .withNewTargetPort()
            .withIntVal(8080)
            .endTargetPort()
            .endPort()
            .withType("LoadBalancer")
            .addToSelector("app", "testApp")
            .addToSelector("tier", "backend")
            .endSpec()
            .build());

    kubernetesService.setControllerPodCount(
        config, Collections.emptyList(), ZONE_CLUSTER, "frontend-ctrl", 0, 2, 10, new ExecutionLogCallback());

    Optional<Integer> backendCount =
        kubernetesService.getControllerPodCount(config, Collections.emptyList(), "backend-ctrl");
    Optional<Integer> frontendCount =
        kubernetesService.getControllerPodCount(config, Collections.emptyList(), "frontend-ctrl");
    logger.info("Controller backend-ctrl has {} instances", backendCount.get());
    logger.info("Controller frontend-ctrl has {} instances", frontendCount.get());

    kubernetesService.checkStatus(config, Collections.emptyList(), "backend-ctrl", "backend-service");
    kubernetesService.checkStatus(config, Collections.emptyList(), "frontend-ctrl", "frontend-service");

    kubernetesService.deleteService(config, Collections.emptyList(), "frontend-service");
    kubernetesService.deleteService(config, Collections.emptyList(), "backend-service");

    kubernetesService.deleteController(config, Collections.emptyList(), "frontend-ctrl");
    kubernetesService.deleteController(config, Collections.emptyList(), "backend-ctrl");

    kubernetesService.checkStatus(config, Collections.emptyList(), "backend-ctrl", "backend-service");
    kubernetesService.checkStatus(config, Collections.emptyList(), "frontend-ctrl", "frontend-service");

    //    gkeClusterService.deleteCluster(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER);
  }
}

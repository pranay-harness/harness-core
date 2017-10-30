/**
 * Copyright (C) 2015 Red Hat, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.wings.integration;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.GcpHelperService.ZONE_DELIMITER;

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

import java.util.List;

@Integration
@Ignore
public class KubernetesIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesIntegrationTest.class);

  private static final SettingAttribute COMPUTE_PROVIDER_SETTING =
      aSettingAttribute()
          .withUuid("GCP_ID")
          .withValue(GcpConfig.builder()
                         .serviceAccountKeyFileContent(KubernetesYamlIntegrationTest.GCP_CONFIG.toCharArray())
                         .build())
          .build();

  private static final String ZONE_CLUSTER = "us-central1-a" + ZONE_DELIMITER + "brett-test";
  private static final String NAMESPACE = "default";

  public static void main(String[] args) throws InterruptedException {
    GkeClusterServiceImpl gkeClusterService = new GkeClusterServiceImpl();
    KubernetesContainerServiceImpl kubernetesService = new KubernetesContainerServiceImpl();

    List<String> clusters = gkeClusterService.listClusters(COMPUTE_PROVIDER_SETTING);
    logger.info("Available clusters: {}", clusters);

    //    KubernetesConfig config = gkeClusterService.createCluster(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER,
    //        ImmutableMap.<String, String>builder()
    //            .put("nodeCount", "1")
    //            .put("machineType", "n1-highcpu-4")
    //            .put("masterUser", "master")
    //            .put("masterPwd", "foo!!bar$$")
    //            .build());

    KubernetesConfig config = gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, NAMESPACE);

    //    gkeClusterService.setNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null, true, 4, 8);
    //    gkeClusterService.setNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null, false, 4, 8);

    NodePoolAutoscaling autoscaling =
        gkeClusterService.getNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null);
    logger.info("Autoscale setting: {}", autoscaling);

    //    kubernetesService.cleanup(config);

    kubernetesService.createController(config,
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
            .withRequests(ImmutableMap.of("cpu", new Quantity("10m"), "memory", new Quantity(("10Mi"))))
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

    kubernetesService.createOrReplaceService(config,
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

    kubernetesService.createController(config,
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

    kubernetesService.createOrReplaceService(config,
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

    kubernetesService.setControllerPodCount(config, ZONE_CLUSTER, "frontend-ctrl", 0, 2, new ExecutionLogCallback());

    int backendCount = kubernetesService.getControllerPodCount(config, "backend-ctrl");
    int frontendCount = kubernetesService.getControllerPodCount(config, "frontend-ctrl");
    logger.info("Controller backend-ctrl has {} instances", backendCount);
    logger.info("Controller frontend-ctrl has {} instances", frontendCount);

    kubernetesService.checkStatus(config, "backend-ctrl", "backend-service");
    kubernetesService.checkStatus(config, "frontend-ctrl", "frontend-service");

    kubernetesService.deleteService(config, "frontend-service");
    kubernetesService.deleteService(config, "backend-service");

    kubernetesService.deleteController(config, "frontend-ctrl");
    kubernetesService.deleteController(config, "backend-ctrl");

    kubernetesService.checkStatus(config, "backend-ctrl", "backend-service");
    kubernetesService.checkStatus(config, "frontend-ctrl", "frontend-service");

    //    gkeClusterService.deleteCluster(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER);
  }
}

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
import static software.wings.service.impl.GcpHelperService.LOCATION_DELIMITER;

import com.google.api.services.container.model.NodePoolAutoscaling;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Integration
@Ignore
public class KubernetesYamlIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesYamlIntegrationTest.class);

  private static final SettingAttribute COMPUTE_PROVIDER_SETTING =
      aSettingAttribute()
          .withUuid("GCP_ID")
          .withValue(GcpConfig.builder().serviceAccountKeyFileContent("GCP_CONFIG".toCharArray()).build())
          .build();

  private static final String ZONE_CLUSTER = "us-central1-a" + LOCATION_DELIMITER + "brett-test";

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
        gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, "default");

    //    gkeClusterService.setNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null, true, 4, 8);
    //    gkeClusterService.setNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null, false, 4, 8);

    NodePoolAutoscaling autoscaling =
        gkeClusterService.getNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, Collections.emptyList(), ZONE_CLUSTER, null);
    logger.info("Autoscale setting: {}", autoscaling);

    //    kubernetesService.cleanup(config);

    String yaml = "---\n"
        + "apiVersion: \"v1\"\n"
        + "kind: \"ReplicationController\"\n"
        + "metadata:\n"
        + "  annotations: {}\n"
        + "  finalizers: []\n"
        + "  labels:\n"
        + "    app: \"testApp\"\n"
        + "    tier: \"backend\"\n"
        + "  name: \"backend-ctrl\"\n"
        + "  ownerReferences: []\n"
        + "spec:\n"
        + "  replicas: 0\n"
        + "  selector: {}\n"
        + "  template:\n"
        + "    metadata:\n"
        + "      annotations: {}\n"
        + "      finalizers: []\n"
        + "      labels:\n"
        + "        app: \"testApp\"\n"
        + "        tier: \"backend\"\n"
        + "      ownerReferences: []\n"
        + "    spec:\n"
        + "      containers:\n"
        + "      - args:\n"
        + "        - \"8080\"\n"
        + "        command: []\n"
        + "        env: []\n"
        + "        image: \"${DOCKER_IMAGE_NAME}\"\n"
        + "        name: \"server\"\n"
        + "        ports:\n"
        + "        - containerPort: 8080\n"
        + "        resources:\n"
        + "          limits:\n"
        + "            cpu: \"100m\"\n"
        + "            memory: \"100Mi\"\n"
        + "          requests:\n"
        + "            cpu: \"10m\"\n"
        + "            memory: \"10Mi\"\n"
        + "        volumeMounts: []\n"
        + "      imagePullSecrets: []\n"
        + "      nodeSelector: {}\n"
        + "      volumes: []\n";

    String rcDefinition = yaml.replace("${DOCKER_IMAGE_NAME}", "gcr.io/exploration-161417/todolist:latest");
    try {
      kubernetesService.createOrReplaceController(
          config, Collections.emptyList(), KubernetesHelper.loadYaml(rcDefinition, ReplicationController.class));
    } catch (IOException e) {
      logger.error("", e);
    }

    yaml = "---\n"
        + "apiVersion: \"v1\"\n"
        + "kind: \"Service\"\n"
        + "metadata:\n"
        + "  annotations: {}\n"
        + "  finalizers: []\n"
        + "  labels:\n"
        + "    app: \"testApp\"\n"
        + "    tier: \"backend\"\n"
        + "  name: \"backend-service\"\n"
        + "  ownerReferences: []\n"
        + "spec:\n"
        + "  deprecatedPublicIPs: []\n"
        + "  externalIPs: []\n"
        + "  loadBalancerSourceRanges: []\n"
        + "  ports:\n"
        + "  - port: 80\n"
        + "    targetPort: 8080\n"
        + "  selector:\n"
        + "    app: \"testApp\"\n"
        + "    tier: \"backend\"\n";
    try {
      kubernetesService.createOrReplaceService(
          config, Collections.emptyList(), KubernetesHelper.loadYaml(yaml, Service.class));
    } catch (IOException e) {
      logger.error("", e);
    }

    yaml = "---\n"
        + "apiVersion: \"v1\"\n"
        + "kind: \"ReplicationController\"\n"
        + "metadata:\n"
        + "  annotations: {}\n"
        + "  finalizers: []\n"
        + "  labels:\n"
        + "    app: \"testApp\"\n"
        + "    tier: \"frontend\"\n"
        + "  name: \"frontend-ctrl\"\n"
        + "  ownerReferences: []\n"
        + "spec:\n"
        + "  replicas: 0\n"
        + "  selector: {}\n"
        + "  template:\n"
        + "    metadata:\n"
        + "      annotations: {}\n"
        + "      finalizers: []\n"
        + "      labels:\n"
        + "        app: \"testApp\"\n"
        + "        tier: \"frontend\"\n"
        + "      ownerReferences: []\n"
        + "    spec:\n"
        + "      containers:\n"
        + "      - args: []\n"
        + "        command: []\n"
        + "        env:\n"
        + "        - name: \"GET_HOSTS_FROM\"\n"
        + "          value: \"dns\"\n"
        + "        image: \"${DOCKER_IMAGE_NAME}\"\n"
        + "        name: \"webapp\"\n"
        + "        ports:\n"
        + "        - containerPort: 8080\n"
        + "        resources:\n"
        + "          limits:\n"
        + "            cpu: \"100m\"\n"
        + "            memory: \"100Mi\"\n"
        + "          requests: {}\n"
        + "        volumeMounts: []\n"
        + "      imagePullSecrets: []\n"
        + "      nodeSelector: {}\n"
        + "      volumes: []\n";
    rcDefinition = yaml.replace("${DOCKER_IMAGE_NAME}", "gcr.io/exploration-161417/todolist:latest");
    try {
      kubernetesService.createOrReplaceController(
          config, Collections.emptyList(), KubernetesHelper.loadYaml(rcDefinition, ReplicationController.class));
    } catch (IOException e) {
      logger.error("", e);
    }

    yaml = "---\n"
        + "apiVersion: \"v1\"\n"
        + "kind: \"Service\"\n"
        + "metadata:\n"
        + "  annotations: {}\n"
        + "  finalizers: []\n"
        + "  labels:\n"
        + "    app: \"testApp\"\n"
        + "    tier: \"frontend\"\n"
        + "  name: \"frontend-service\"\n"
        + "  ownerReferences: []\n"
        + "spec:\n"
        + "  deprecatedPublicIPs: []\n"
        + "  externalIPs: []\n"
        + "  loadBalancerSourceRanges: []\n"
        + "  ports:\n"
        + "  - port: 80\n"
        + "    targetPort: 8080\n"
        + "  selector:\n"
        + "    app: \"testApp\"\n"
        + "    tier: \"backend\"\n"
        + "  type: \"LoadBalancer\"";
    try {
      kubernetesService.createOrReplaceService(
          config, Collections.emptyList(), KubernetesHelper.loadYaml(yaml, Service.class));
    } catch (IOException e) {
      logger.error("", e);
    }

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

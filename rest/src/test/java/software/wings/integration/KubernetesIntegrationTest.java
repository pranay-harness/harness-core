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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Integration
@Ignore
public class KubernetesIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesIntegrationTest.class);
  private static final String GCP_CONFIG_CONTENT = "{\n"
      + "  \"type\": \"service_account\",\n"
      + "  \"project_id\": \"exploration-161417\",\n"
      + "  \"private_key_id\": \"c3cebea25120157ff4f16309f5a3894ae6aac964\",\n"
      + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\n"
      + "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCdYqaNvPh+f26q\\n"
      + "55I0mXR0s4W7y8Rk+6lM8ZHwCFuEM6D3W4kNQDlYJ7QVaSmVkzE7XikwMp7/bbSG\\n"
      + "dOcoB00gn5zGD92C6sZu8Szb0UFhU8nDbwXDlBcx1M1rrOyXtLd0+KLeKDloUgO/\\n"
      + "0QVh+AWeHETyw1JxG77knsnxTfiXbjjJ6bxlbPboD6PLmM6wntPUqcrvXICG6uov\\n"
      + "xyAjsJylKMraaulguWMOex1EpKjYvvMcONG4ko23QrE0YqROg5PsvqegAWapD6Fw\\n"
      + "QL23Shjl+HsESOnzpeYGKVH2KsihDXLu5lSjHGBPzzwvg2N+F/+40+UFuvrgwxNu\\n"
      + "dRJTN0/5AgMBAAECggEAY12ibECP30XTeEGmVGFCXl/tokifYWZmWHb4PcT5CrmR\\n"
      + "+joniF8xFVBT6WSw5Ye+AI6NkKmVKw13eaCbRPF/J4a+c2oW344c5HSObuZp0eoV\\n"
      + "q7cPu5BnKIYDf+T4pztozIgiRAK/Y/bL+TIdpOHzogSVH3RkO5dZ6Xu7YgdpWSk3\\n"
      + "XCMPFKYILMwEyECSRs/ryFUGnfvXzTO4FSjLkVuLyvUSz0Vd5itIXbnbSqUM7V33\\n"
      + "39gtZJzq0nZxA5mTtNAlDaSKnxo53rNx4EPJjjsSDL2AT9GzWMdkeGJAK0yTkNuR\\n"
      + "gC2p60sjGhgZd56DVPkNEYUNoozFssV3RLA0zhc3UQKBgQDM1YQ66EU9ZCcq0WQV\\n"
      + "kB/ykO59ythmw6vIwFWgvRelOZpuvrhDz1jOFIbjDaYo1nkhBdOLRKYiTcL7dKsN\\n"
      + "iktvkMM67OY0C41hf+UYHa6EhH6+V/UNA38aGe3WLQQ3NqfEeLHKKLktp4GasV7L\\n"
      + "R01j0APY6UhgNKWNPr7UZEd3/QKBgQDEsvEHHJGsEmslFgOufspoRRG4ylf5581s\\n"
      + "hyDD2MQIiVdYqO8RbDkfwde2G0yIhQYEyPb/FkGkmj0PfcKLUwQ3G3XVP/3b7F3S\\n"
      + "wJcUlrbrFJ33w2lIDQWSHnESb6N3UOyntplApH1g/ZPcwPG5q55Q4dbTyw+qiE9g\\n"
      + "/TXm1KxCrQKBgQCOwLeY/ktTD7ukQa4IwRsiyBMOJBJQ/El6bWC/10jlY5HXYJ+2\\n"
      + "0ojHhtLC5r5Ic8CTXSYjR0KpYZxj9tlHZHxSUoddR8DfwLVVn/afqf/4ZwaVzWMB\\n"
      + "INCx9iQlQdZQTIz7hkoR4/O6d6UBlF/GN/kdeNlVkK9aLRa7q2D/UKvmWQKBgGv5\\n"
      + "7uYAgNb8pWSOWScI4wOqJrhSG0lMPjA9XXclHzewbQp2cgYWaqVMO6X0BmdK3qrx\\n"
      + "xuTwysgZAzvlxU3GHKJDqMOPhUOc1UREBKuAsJkLkEvaMYkj3NMBcwCz2AA/pCnM\\n"
      + "ywP5R/peOTUNlaRe2WF9F/jbl5X9jdWoKla0mHthAoGBAL74vsUNr3/Px8oItAHT\\n"
      + "QWAcQmAk+fV/xurP8LGBBoiu/JYzTJPtvBHVxpoXafmhD/ct66YG5hmnDJlVBzZe\\n"
      + "Wj86UJtLNgSayoTRRMrmMQnkgZJLvPAf+HaJuNk9xvSI9jIi5gc/5TFEBx2dNnNU\\n"
      + "sb5o1VMkdhFcc6MoSa25gjzJ\\n-----END PRIVATE KEY-----\\n\",\n"
      + "  \"client_email\": \"wings-58@exploration-161417.iam.gserviceaccount.com\",\n"
      + "  \"client_id\": \"118438022149431161950\",\n"
      + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
      + "  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
      + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
      + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/wings-58%40exploration-161417.iam.gserviceaccount.com\"\n"
      + "}";

  private static final SettingAttribute COMPUTE_PROVIDER_SETTING =
      aSettingAttribute()
          .withUuid("GCP_ID")
          .withValue(GcpConfig.builder().serviceAccountKeyFileContent(GCP_CONFIG_CONTENT.toCharArray()).build())
          .build();

  private static final String ZONE_CLUSTER = "us-central1-a" + ZONE_DELIMITER + "brett-test";
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

    kubernetesService.createController(config, Collections.emptyList(),
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

    kubernetesService.createController(config, Collections.emptyList(),
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

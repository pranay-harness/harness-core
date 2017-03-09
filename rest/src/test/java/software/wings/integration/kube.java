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

import com.google.api.services.container.model.NodePoolAutoscaling;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterServiceImpl;
import software.wings.cloudprovider.gke.KubernetesContainerServiceImpl;

import java.util.List;

import static software.wings.beans.GcpConfig.GcpConfigBuilder.aGcpConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.GcpHelperService.ZONE_DELIMITER;

public class kube {
  private static final Logger logger = LoggerFactory.getLogger(kube.class);

  private static final SettingAttribute COMPUTE_PROVIDER_SETTING =
      aSettingAttribute()
          .withValue(
              aGcpConfig()
                  .withServiceAccountKeyFileContent("{\n"
                      + "  \"type\": \"service_account\",\n"
                      + "  \"project_id\": \"kubernetes-test-158122\",\n"
                      + "  \"private_key_id\": \"7cd7ca86464ae486243fbdd43942251f98ba978e\",\n"
                      + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\n"
                      + "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC9tKjXGrlF0wH6\\n"
                      + "dV9S/4VqYamImpQDqleeWH+M+WqjejROZuLsxoiaUzvGaqsMejaDhgx+gAc4PVRR\\n"
                      + "jQolKxYd751mF+bLb5zbnjAuW1yXOirBdx22hRC23fHab1w9a4M0KmDqiBv7dW+6\\n"
                      + "xEMuTR0VQyggFUWskr5KSjTn3m87VnXH0sXBWcYszopb3/uLLUc4Z8lHrBFVl6X+\\n"
                      + "EpHNVk6ZmhY1LvPgZx17M12O9N+uWtRTZsRzOcomENsXhnKGNOYseJh69e0WX0iF\\n"
                      + "MdT+CukcXqZlEHj4eI5JQeEKZar/q0NVluFpCKh3cba04cCzzU6Go6RdggoCaw88\\n"
                      + "yGX0jZQ1AgMBAAECggEAQAs2mO/3rl/vIgvv84g6FVEFOA3ffGFMlTORVBl769a8\\n"
                      + "w98uKvtXcKo62uB2AI9dygc+PpKgXVcvGR0BWMzz+YVEYQweqX1zuhzsbSoA1zkW\\n"
                      + "6Bt7l3vSybmiBblkrYOXVswbzfHHaJwb7TSG7aGw+NWkPTiRPhKIVXI57DGG4cPY\\n"
                      + "zdSw6SUvoBWu5AdEZ6I96GYkQ4hj89S4+WxpFtDLHOWAIj/5KQTYtOLOhHblXT1u\\n"
                      + "MT4Iu9keFFeq65v5xAQdnt4jS3igjdOOzYrH+tyfVrgGP8oga7XpcYJN5GbwCYBE\\n"
                      + "NKgNzqcxxq5IR6HgnBfaMLRKlRU/L+CDyi3b04NQIQKBgQDfb2SkMXz1rP3b/QpG\\n"
                      + "W281aNx6bzNOr17tjyoet8GWvPbA81M9+Y2lbuSqCLo9VBR+uA0+ZSQaUjucImG2\\n"
                      + "R74RlYzDJng68NHLesBbqWEiTF8yukUVfhSd4X4ss9I4UsmWMIRWaT4xaaVo1y5/\\n"
                      + "6CHsbsk8TPmFlREA/iUMk66XPQKBgQDZWsjtwx6Jyb9z6E2zoCe282alBAoDJygh\\n"
                      + "Y36zIATci1jpEyhxMgwAQrHqKF0GKMQ0yOaO6GyPVPPV6pGz/oa7j/5DcMssJ1VR\\n"
                      + "Pq2H791SR+NwkYIh7rHF2vPzBQc38BGSDxLckFqtIuzjMou2p36OKMOKJbWgxhQp\\n"
                      + "T9kAhfcAWQKBgHawGHjz5NooK625K8UV+uwXKM9M1KdelwGARDKPECG/fSAf4T8R\\n"
                      + "mkGft43vFudPC5gIsthLJ7NnrUySIu+OCpQSqfRcMg+1Lux050uJnRR4FzW+JsZ0\\n"
                      + "9ASt7LVYTDopF/ZVDWdNfoEHCpu3enbtW1/ZtcH3bbCc76xkVE/q/xpNAoGBAIB+\\n"
                      + "wzqMl4kt1BlZxFXk4JWv34a+lIy4oWjbwRN0Ymtflfh0cvw4cg/VXgjoQ9ZYU2ZB\\n"
                      + "PsxNa6BwwJY+TlTyrARGZDLKg9JejnmxbDVpAJacGUF7REt7KW2mu4F3/4R6UGjg\\n"
                      + "sG3kiTbB5vmJ8D7TVmYEg1UwZefeMh0aL1e32wZBAoGBAJl8sU8lxJ6pmZzYXfsY\\n"
                      + "8rFyNI8hcEwGXhRFS3ixP5xOe6nzgh0Ldw+SENtmn6SVzNZZ/JV7uKw2+AycjI4i\\n"
                      + "vOkLbFMlbJrFJmzdyvF1oZONFcQglEimTb5j5zZiaO3P8FXzEkAOlkdBcvUMOqUl\\n"
                      + "GI4nRF0qYe1J6ohNe1ObY1ES\\n-----END PRIVATE KEY-----\\n\",\n"
                      + "  \"client_email\": \"752449710621-compute@developer.gserviceaccount.com\",\n"
                      + "  \"client_id\": \"112553346666451790549\",\n"
                      + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
                      + "  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
                      + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
                      + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/752449710621-compute%40developer.gserviceaccount.com\"\n"
                      + "}")
                  .build())
          .build();

  private static final String ZONE_CLUSTER = "us-west1-a" + ZONE_DELIMITER + "baz-qux";

  public static void main(String[] args) throws InterruptedException {
    GkeClusterServiceImpl gkeClusterService = new GkeClusterServiceImpl();
    KubernetesContainerServiceImpl kubernetesService = new KubernetesContainerServiceImpl();

    List<String> clusters = gkeClusterService.listClusters(COMPUTE_PROVIDER_SETTING);
    logger.info("Available clusters: {}", clusters);

    //    KubernetesConfig config = gkeClusterService.createCluster(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER,
    //        ImmutableMap.<String, String>builder()
    //            .put("nodeCount", "1")
    //            .put("masterUser", "master")
    //            .put("masterPwd", "foo!!bar$$")
    //            .build());

    KubernetesConfig config = gkeClusterService.getCluster(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER);

    //    gkeClusterService.setNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null, true, 4, 8);

    NodePoolAutoscaling autoscaling =
        gkeClusterService.getNodePoolAutoscaling(COMPUTE_PROVIDER_SETTING, ZONE_CLUSTER, null);
    logger.info("Autoscale setting: {}", autoscaling);

    kubernetesService.cleanup(config);

    ReplicationController rc =
        new ReplicationControllerBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName("backend-ctrl")
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .withReplicas(2)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels("app", "testApp")
            .addToLabels("tier", "backend")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("server")
            .withImage("gcr.io/gdg-apps-1090/graphviz-server")
            .withArgs("8080")
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
            .build();

    kubernetesService.createController(config, rc);

    kubernetesService.createService(config,
        ImmutableMap.<String, String>builder()
            .put("name", "backend-service")
            .put("appName", "testApp")
            .put("tier", "backend")
            .put("port", "80")
            .put("targetPort", "8080")
            .build());

    rc = new ReplicationControllerBuilder()
             .withApiVersion("v1")
             .withNewMetadata()
             .withName("frontend-ctrl")
             .addToLabels("app", "testApp")
             .addToLabels("tier", "frontend")
             .endMetadata()
             .withNewSpec()
             .withReplicas(2)
             .withNewTemplate()
             .withNewMetadata()
             .addToLabels("app", "testApp")
             .addToLabels("tier", "frontend")
             .endMetadata()
             .withNewSpec()
             .addNewContainer()
             .withName("webapp")
             .withImage("gcr.io/gdg-apps-1090/graphviz-webapp")
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
             .build();

    kubernetesService.createController(config, rc);

    kubernetesService.createService(config,
        ImmutableMap.<String, String>builder()
            .put("name", "frontend-service")
            .put("appName", "testApp")
            .put("tier", "frontend")
            .put("type", "LoadBalancer")
            .put("port", "80")
            .put("targetPort", "8080")
            .build());

    kubernetesService.setControllerPodCount(config, "frontend-ctrl", 5);

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

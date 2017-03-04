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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterServiceImpl;
import software.wings.cloudprovider.gke.KubernetesContainerServiceImpl;

import java.util.List;

public class kube {
  private static final Logger logger = LoggerFactory.getLogger(kube.class);

  private static final String CREDS = "{\n"
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
      + "GI4nRF0qYe1J6ohNe1ObY1ES\\n"
      + "-----END PRIVATE KEY-----\\n\",\n"
      + "  \"client_email\": \"752449710621-compute@developer.gserviceaccount.com\",\n"
      + "  \"client_id\": \"112553346666451790549\",\n"
      + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
      + "  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
      + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
      + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/752449710621-compute%40developer.gserviceaccount.com\"\n"
      + "}";

  public static void main(String[] args) throws InterruptedException {
    GkeClusterServiceImpl gkeClusterService = new GkeClusterServiceImpl();
    KubernetesContainerServiceImpl kubernetesService = new KubernetesContainerServiceImpl();

    ImmutableMap<String, String> projectParams = ImmutableMap.<String, String>builder()
                                                     .put("credentials", CREDS)
                                                     .put("projectId", "kubernetes-test-158122")
                                                     .put("appName", "testApp")
                                                     .put("zone", "us-west1-a")
                                                     .build();

    ImmutableMap<String, String> clusterParams =
        ImmutableMap.<String, String>builder().putAll(projectParams).put("name", "baz-qux").build();

    List<String> clusters = gkeClusterService.listClusters(projectParams);
    logger.info("Available clusters: {}", clusters);

    //    KubernetesConfig config = gkeClusterService.createCluster(
    //        ImmutableMap.<String, String>builder()
    //            .putAll(clusterParams)
    //            .put("nodeCount", "1")
    //            .put("masterUser", "master")
    //            .put("masterPwd", "foo!!bar$$")
    //            .build());

    KubernetesConfig config = gkeClusterService.getCluster(clusterParams);

    //    gkeClusterService.setNodePoolAutoscaling(true, 4, 8, clusterParams);

    NodePoolAutoscaling autoscaling = gkeClusterService.getNodePoolAutoscaling(clusterParams);
    logger.info("Autoscale setting: {}", autoscaling);

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute().withValue(config).build();

    kubernetesService.cleanup(settingAttribute);

    kubernetesService.createController(settingAttribute,
        ImmutableMap.<String, String>builder()
            .put("name", "backend-ctrl")
            .put("appName", "testApp")
            .put("containerName", "server")
            .put("containerImage", "gcr.io/gdg-apps-1090/graphviz-server")
            .put("tier", "backend")
            .put("cpu", "100m")
            .put("memory", "100Mi")
            .put("port", "8080")
            .put("count", "2")
            .build());

    kubernetesService.createService(settingAttribute,
        ImmutableMap.<String, String>builder()
            .put("name", "backend-service")
            .put("appName", "testApp")
            .put("tier", "backend")
            .put("port", "80")
            .put("targetPort", "8080")
            .build());

    kubernetesService.createController(settingAttribute,
        ImmutableMap.<String, String>builder()
            .put("name", "frontend-ctrl")
            .put("appName", "testApp")
            .put("containerName", "webapp")
            .put("containerImage", "gcr.io/gdg-apps-1090/graphviz-webapp")
            .put("tier", "frontend")
            .put("cpu", "100m")
            .put("memory", "100Mi")
            .put("port", "8080")
            .put("count", "2")
            .build());

    kubernetesService.createService(settingAttribute,
        ImmutableMap.<String, String>builder()
            .put("name", "frontend-service")
            .put("appName", "testApp")
            .put("tier", "frontend")
            .put("type", "LoadBalancer")
            .put("port", "80")
            .put("targetPort", "8080")
            .build());

    kubernetesService.setControllerPodCount(settingAttribute, "frontend-ctrl", 5);

    int backendCount = kubernetesService.getControllerPodCount(settingAttribute, "backend-ctrl");
    int frontendCount = kubernetesService.getControllerPodCount(settingAttribute, "frontend-ctrl");
    logger.info("Controller backend-ctrl has {} instances", backendCount);
    logger.info("Controller frontend-ctrl has {} instances", frontendCount);

    kubernetesService.checkStatus(settingAttribute, "backend-ctrl", "backend-service");
    kubernetesService.checkStatus(settingAttribute, "frontend-ctrl", "frontend-service");

    kubernetesService.deleteService(settingAttribute, "frontend-service");
    kubernetesService.deleteService(settingAttribute, "backend-service");

    kubernetesService.deleteController(settingAttribute, "frontend-ctrl");
    kubernetesService.deleteController(settingAttribute, "backend-ctrl");

    kubernetesService.checkStatus(settingAttribute, "backend-ctrl", "backend-service");
    kubernetesService.checkStatus(settingAttribute, "frontend-ctrl", "frontend-service");

    //    gkeClusterService.deleteCluster(
    //        ImmutableMap.of(
    //            "name", "foo-bar",
    //            "appName", "testApp",
    //            "projectId", "kubernetes-test-158122",
    //            "zone", "us-west1-a"));
  }
}

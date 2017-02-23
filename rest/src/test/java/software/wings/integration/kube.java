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

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterServiceImpl;
import software.wings.cloudprovider.gke.KubernetesContainerServiceImpl;

public class kube {
  private static final Logger logger = LoggerFactory.getLogger(kube.class);

  public static void main(String[] args) throws InterruptedException {
    GkeClusterServiceImpl gkeClusterService = new GkeClusterServiceImpl();
    KubernetesContainerServiceImpl kubernetesService = new KubernetesContainerServiceImpl();

    KubernetesConfig config = gkeClusterService.createCluster(ImmutableMap.<String, String>builder()
                                                                  .put("name", "foo-bar")
                                                                  .put("projectId", "kubernetes-test-158122")
                                                                  .put("appName", "testApp")
                                                                  .put("zone", "us-west1-a")
                                                                  .put("nodeCount", "1")
                                                                  .put("masterUser", "master")
                                                                  .put("masterPwd", "foo!!bar$$")
                                                                  .build());

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
    logger.info(String.format("Controller backend-ctrl has %d instances", backendCount));
    logger.info(String.format("Controller frontend-ctrl has %d instances", frontendCount));

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

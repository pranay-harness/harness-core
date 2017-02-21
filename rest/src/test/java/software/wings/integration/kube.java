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
import software.wings.cloudprovider.kubernetes.KubernetesContainerServiceImpl;

public class kube {
  public static void main(String[] args) throws InterruptedException {
    KubernetesContainerServiceImpl kubernetesService = new KubernetesContainerServiceImpl();

    kubernetesService.createCluster(ImmutableMap.<String, String>builder()
                                        .put("name", "foo-bar")
                                        .put("projectId", "kubernetes-test-158122")
                                        .put("appName", "testApp")
                                        .put("zone", "us-west1-a")
                                        .put("nodeCount", "1")
                                        .put("masterUser", "master")
                                        .put("masterPwd", "foo!!bar$$")
                                        .build());

    kubernetesService.cleanup();

    kubernetesService.createController(ImmutableMap.<String, String>builder()
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

    kubernetesService.createService(
        ImmutableMap.of("name", "backend-service", "appName", "testApp", "tier", "backend"));

    kubernetesService.createController(ImmutableMap.<String, String>builder()
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

    kubernetesService.createService(
        ImmutableMap.of("name", "frontend-service", "appName", "testApp", "tier", "frontend", "type", "LoadBalancer"));

    kubernetesService.setControllerPodCount("frontend-ctrl", 5);

    kubernetesService.checkStatus("backend-ctrl", "backend-service");
    kubernetesService.checkStatus("frontend-ctrl", "frontend-service");

    //    kubernetesService.destroyCluster(
    //        ImmutableMap.of(
    //            "name", "foo-bar",
    //            "projectId", "kubernetes-test-158122",
    //            "zone", "us-west1-a"));
  }
}

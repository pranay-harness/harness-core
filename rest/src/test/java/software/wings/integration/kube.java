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
import io.fabric8.kubernetes.api.model.Quantity;
import software.wings.cloudprovider.kubernetes.KubernetesServiceImpl;

public class kube {
  public static void main(String[] args) throws InterruptedException {
    KubernetesServiceImpl kubernetesService = new KubernetesServiceImpl();

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

    //    kubernetesService.destroyCluster(
    //        ImmutableMap.of(
    //            "name", "foo-bar",
    //            "projectId", "kubernetes-test-158122",
    //            "zone", "us-west1-a"));
  }
}

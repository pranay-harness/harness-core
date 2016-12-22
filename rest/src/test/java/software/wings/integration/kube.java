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

public class kube {
  private static final Logger logger = LoggerFactory.getLogger(kube.class);

  public static void main(String[] args) throws InterruptedException {
    String master = "https://104.197.5.243";
    if (args.length == 1) {
      master = args[0];
    }

    Config config = new ConfigBuilder()
                        .withMasterUrl(master)
                        .withTrustCerts(true)
                        .withUsername("admin")
                        .withPassword("2D12D5RozsEp3n3o")
                        .withNamespace("default")
                        .build();

    try (final KubernetesClient client = new DefaultKubernetesClient(config)) {
      log(client.namespaces().list().toString());

      cleanup(client);
      //      createBackendController(client);
      //      createFrontendController(client);
      //      scaleFrontendController(client, 5);
      //      createBackendService(client);
      //      createFrontendService(client);

      client.nodes().list();

      // create from yml files

      //      createReplicationController(client, "/Users/anubhaw/work/kubernetes-graphviz/backend-controller.yaml");
      //      createReplicationController(client, "/Users/anubhaw/work/kubernetes-graphviz/frontend-controller.yaml");
      //      createService(client, "/Users/anubhaw/work/kubernetes-graphviz/backend-service.yaml");
      //      createService(client, "/Users/anubhaw/work/kubernetes-graphviz/frontend-service.yaml");

    } catch (Exception e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);

      Throwable[] suppressed = e.getSuppressed();
      if (suppressed != null) {
        for (Throwable t : suppressed) {
          logger.error(t.getMessage(), t);
        }
      }
    }
  }

  private static void createService(KubernetesClient client, String configFilePath) {
    Service service = client.services().load(configFilePath).create();
    log("Created service", service);
  }

  private static void createReplicationController(KubernetesClient client, String configFilePath) {
    ReplicationController replicationController = client.replicationControllers().load(configFilePath).create();
    log("Created replication controller", replicationController);
  }

  private static void cleanup(KubernetesClient client) {
    client.services().delete();
    client.replicationControllers().delete();
  }

  private static void createFrontendService(KubernetesClient client) {
    Service service = client.services()
                          .createNew()
                          .withApiVersion("v1")
                          .withNewMetadata()
                          .withName("frontend-service")
                          .addToLabels("app", "graphviz")
                          .addToLabels("tier", "frontend")
                          .endMetadata()
                          .withNewSpec()
                          .withType("LoadBalancer")
                          .addNewPort()
                          .withPort(80)
                          .withNewTargetPort()
                          .withIntVal(8080)
                          .endTargetPort()
                          .endPort()
                          .addToSelector("app", "graphviz")
                          .addToSelector("tier", "frontend")
                          .endSpec()
                          .done();
    log("Created frontend service " + service.toString());
  }

  private static void createBackendService(KubernetesClient client) {
    Service service = client.services()
                          .createNew()
                          .withApiVersion("v1")
                          .withNewMetadata()
                          .withName("backend-service")
                          .addToLabels("app", "graphviz")
                          .addToLabels("tier", "backend")
                          .endMetadata()
                          .withNewSpec()
                          .addNewPort()
                          .withPort(80)
                          .withNewTargetPort()
                          .withIntVal(8080)
                          .endTargetPort()
                          .endPort()
                          .addToSelector("app", "graphviz")
                          .addToSelector("tier", "backend")
                          .endSpec()
                          .done();
    log("Created backend service " + service.toString());
  }

  private static void scaleFrontendController(KubernetesClient client, int number) {
    ReplicationController scale = client.replicationControllers().withName("frontend-contr").scale(number);
    log(String.format("Frontend-contrl scaled to %s pods", number));
  }

  private static void createFrontendController(KubernetesClient client) {
    ResourceRequirements resourceRequirements = new ResourceRequirements();
    resourceRequirements.setRequests(ImmutableMap.of("cpu", new Quantity("100m"), "memory", new Quantity("100Mi")));

    ReplicationController rc = new ReplicationControllerBuilder()
                                   .withApiVersion("v1")
                                   .withNewMetadata()
                                   .withName("frontend-contr")
                                   .addToLabels("app", "graphviz")
                                   .addToLabels("tier", "frontend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .withReplicas(3)
                                   .withNewTemplate()
                                   .withNewMetadata()
                                   .addToLabels("app", "graphviz")
                                   .addToLabels("tier", "frontend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .addNewContainer()
                                   .withName("webapp")
                                   .withImage("omerio/graphviz-webapp")
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

    log("Created Front end controller", client.replicationControllers().inNamespace("default").create(rc));
  }

  private static void createBackendController(KubernetesClient client) {
    ResourceRequirements resourceRequirements = new ResourceRequirements();
    resourceRequirements.setRequests(ImmutableMap.of("cpu", new Quantity("100m"), "memory", new Quantity("100Mi")));

    ReplicationController rc = new ReplicationControllerBuilder()
                                   .withApiVersion("v1")
                                   .withNewMetadata()
                                   .withName("backend-contr")
                                   .addToLabels("app", "graphviz")
                                   .addToLabels("tier", "backend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .withReplicas(2)
                                   .withNewTemplate()
                                   .withNewMetadata()
                                   .addToLabels("app", "graphviz")
                                   .addToLabels("tier", "backend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .addNewContainer()
                                   .withName("server")
                                   .withImage("omerio/graphviz-server")
                                   .withArgs("8080")
                                   .withResources(resourceRequirements)
                                   .addNewPort()
                                   .withContainerPort(8080)
                                   .endPort()
                                   .endContainer()
                                   .endSpec()
                                   .endTemplate()
                                   .endSpec()
                                   .build();

    log("Created Backend controller", client.replicationControllers().inNamespace("default").create(rc));
  }

  private static void log(String action, Object obj) {
    logger.info("{}: {}", action, obj);
  }

  private static void log(String action) {
    logger.info(action);
  }
}

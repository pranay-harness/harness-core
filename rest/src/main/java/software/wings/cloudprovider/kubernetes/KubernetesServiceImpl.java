package software.wings.cloudprovider.kubernetes;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.CreateNodePoolRequest;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
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
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

/**
 * Created by brett on 2/9/17.
 */
@Singleton
public class KubernetesServiceImpl implements KubernetesService {
  private static final int SLEEP_INTERVAL = 5 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private KubernetesClient client;

  public void createCluster(Map<String, String> params) throws InterruptedException {
    Container containerService = getContainerService(params.get("appName"));

    Cluster cluster = null;
    try {
      cluster = containerService.projects()
                    .zones()
                    .clusters()
                    .get(params.get("projectId"), params.get("zone"), params.get("name"))
                    .execute();
      logger.info("Cluster " + params.get("name") + " already exists in zone " + params.get("zone") + " for project "
          + params.get("projectId"));
    } catch (IOException e) {
      boolean notFound = false;
      if (e instanceof GoogleJsonResponseException) {
        GoogleJsonResponseException ex = (GoogleJsonResponseException) e;
        if (ex.getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
          notFound = true;
          logger.info("Cluster " + params.get("name") + " does not exist in zone " + params.get("zone")
              + " for project " + params.get("projectId"));
        }
      }
      if (!notFound) {
        e.printStackTrace();
      }
    }

    if (cluster == null) {
      cluster = new Cluster()
                    .setName(params.get("name"))
                    .setInitialNodeCount(Integer.valueOf(params.get("nodeCount")))
                    .setMasterAuth(
                        new MasterAuth().setUsername(params.get("masterUser")).setPassword(params.get("masterPwd")));

      CreateClusterRequest content = new CreateClusterRequest().setCluster(cluster);

      Operation response = null;
      try {
        response = containerService.projects()
                       .zones()
                       .clusters()
                       .create(params.get("projectId"), params.get("zone"), content)
                       .execute();
      } catch (IOException e) {
        e.printStackTrace();
      }

      logger.info("Provisioning...");
      int i = 0;
      while (response.getStatus().equals("RUNNING")) {
        Thread.sleep(SLEEP_INTERVAL);
        try {
          response = containerService.projects()
                         .zones()
                         .operations()
                         .get(params.get("projectId"), params.get("zone"), response.getName())
                         .execute();
        } catch (IOException e) {
          e.printStackTrace();
        }
        i += 5;
        logger.info("Provisioning... " + i);
      }
      logger.info("Provisioning: " + response.getStatus());

      try {
        cluster = containerService.projects()
                      .zones()
                      .clusters()
                      .get(params.get("projectId"), params.get("zone"), params.get("name"))
                      .execute();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    logger.info("Cluster status: " + cluster.getStatus());
    logger.info("Master endpoint: " + cluster.getEndpoint());

    client = new DefaultKubernetesClient(new ConfigBuilder()
                                             .withMasterUrl("https://" + cluster.getEndpoint())
                                             .withTrustCerts(true)
                                             .withUsername(params.get("masterUser"))
                                             .withPassword(params.get("masterPwd"))
                                             .withNamespace("default")
                                             .build());

    logger.info("Connected to cluster");
  }

  public void destroyCluster(Map<String, String> params) throws InterruptedException {
    Container containerService = getContainerService(params.get("appName"));

    Operation response = null;
    try {
      response = containerService.projects()
                     .zones()
                     .clusters()
                     .delete(params.get("projectId"), params.get("zone"), params.get("name"))
                     .execute();
    } catch (IOException e) {
      e.printStackTrace();
    }

    logger.info("Deleting cluster...");
    int i = 0;
    while (response.getStatus().equals("RUNNING")) {
      Thread.sleep(SLEEP_INTERVAL);
      try {
        response = containerService.projects()
                       .zones()
                       .operations()
                       .get(params.get("projectId"), params.get("zone"), response.getName())
                       .execute();
      } catch (IOException e) {
        e.printStackTrace();
      }
      i += 5;
      logger.info("Deleting cluster... " + i);
    }
    logger.info("Delete cluster: " + response.getStatus());
  }

  private Container getContainerService(String appName) {
    GoogleCredential credential = null;
    try {
      credential = GoogleCredential.getApplicationDefault();
      if (credential.createScopedRequired()) {
        credential = credential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    Container containerService = null;
    try {
      containerService =
          new Container
              .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential)
              .setApplicationName(appName)
              .build();
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return containerService;
  }

  @Override
  public void provisionNodes(SettingAttribute connectorConfig, String autoScalingGroupName, Integer clusterSize) {
    CreateNodePoolRequest request;
    KubernetesConfig kubernetesConfig = (KubernetesConfig) connectorConfig.getValue();
    Config config = new ConfigBuilder()
                        .withMasterUrl(kubernetesConfig.getApiServerUrl())
                        .withUsername(kubernetesConfig.getUsername())
                        .withPassword(kubernetesConfig.getPassword())
                        .build();

    client = new DefaultKubernetesClient(config);
    for (int i = 0; i < clusterSize; i++) {
      client.nodes().createNew().done();
    }
  }

  @Override
  public void provisionNodes(
      SettingAttribute connectorConfig, Integer clusterSize, String launchConfigName, Map<String, Object> params) {
    KubernetesConfig kubernetesConfig = (KubernetesConfig) connectorConfig.getValue();
    Config config = new ConfigBuilder()
                        .withMasterUrl(kubernetesConfig.getApiServerUrl())
                        .withUsername(kubernetesConfig.getUsername())
                        .withPassword(kubernetesConfig.getPassword())
                        .build();

    client = new DefaultKubernetesClient(config);
    for (int i = 0; i < clusterSize; i++) {
      client.nodes().createNew().done();
    }
  }

  @Override
  public String deployService(SettingAttribute connectorConfig, String serviceDefinition) {
    return null;
  }

  @Override
  public void deleteService(SettingAttribute connectorConfig, String clusterName, String serviceName) {}

  @Override
  public void createFrontendService(Map<String, String> params) {
    client.services()
        .createOrReplaceWithNew()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(params.get("name"))
        .addToLabels("app", params.get("appName"))
        .addToLabels("tier", "frontend")
        .endMetadata()
        .withNewSpec()
        .withType(params.get("type"))
        .addNewPort()
        .withPort(80)
        .withNewTargetPort()
        .withIntVal(8080)
        .endTargetPort()
        .endPort()
        .addToSelector("app", params.get("appName"))
        .addToSelector("tier", "frontend")
        .endSpec()
        .done();
    logger.info("Created frontend service " + params.get("name") + " for " + params.get("appName"));
  }

  @Override
  public void createBackendService(Map<String, String> params) {
    client.services()
        .createOrReplaceWithNew()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(params.get("name"))
        .addToLabels("app", params.get("appName"))
        .addToLabels("tier", "backend")
        .endMetadata()
        .withNewSpec()
        .addNewPort()
        .withPort(80)
        .withNewTargetPort()
        .withIntVal(8080)
        .endTargetPort()
        .endPort()
        .addToSelector("app", params.get("appName"))
        .addToSelector("tier", "backend")
        .endSpec()
        .done();
    logger.info("Created backend service " + params.get("name") + " for " + params.get("appName"));
  }

  @Override
  public void createFrontendController(Map<String, Quantity> requests, Map<String, String> params) {
    ResourceRequirements resourceRequirements = new ResourceRequirements();
    resourceRequirements.setRequests(requests);

    ReplicationController rc = new ReplicationControllerBuilder()
                                   .withApiVersion("v1")
                                   .withNewMetadata()
                                   .withName(params.get("name"))
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "frontend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .withReplicas(Integer.valueOf(params.get("count")))
                                   .withNewTemplate()
                                   .withNewMetadata()
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "frontend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .addNewContainer()
                                   .withName("webapp")
                                   .withImage(params.get("webappImage"))
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

    client.replicationControllers().inNamespace("default").createOrReplace(rc);
    logger.info("Created frontend controller " + params.get("name") + " for " + params.get("appName"));
  }

  @Override
  public void scaleFrontendController(String name, int number) {
    client.replicationControllers().withName(name).scale(number);
    logger.info("Scaled frontend controller " + name + " to " + number + " instances");
  }

  @Override
  public void createBackendController(Map<String, String> params) {
    ResourceRequirements resourceRequirements = new ResourceRequirements();
    resourceRequirements.setRequests(
        ImmutableMap.of("cpu", new Quantity(params.get("cpu")), "memory", new Quantity(params.get("memory"))));

    ReplicationController rc = new ReplicationControllerBuilder()
                                   .withApiVersion("v1")
                                   .withNewMetadata()
                                   .withName(params.get("name"))
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "backend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .withReplicas(Integer.valueOf(params.get("count")))
                                   .withNewTemplate()
                                   .withNewMetadata()
                                   .addToLabels("app", params.get("appName"))
                                   .addToLabels("tier", "backend")
                                   .endMetadata()
                                   .withNewSpec()
                                   .addNewContainer()
                                   .withName("server")
                                   .withImage(params.get("serverImage"))
                                   .withArgs(params.get("port"))
                                   .withResources(resourceRequirements)
                                   .addNewPort()
                                   .withContainerPort(8080)
                                   .endPort()
                                   .endContainer()
                                   .endSpec()
                                   .endTemplate()
                                   .endSpec()
                                   .build();

    client.replicationControllers().inNamespace("default").createOrReplace(rc);
    logger.info("Created backend controller " + params.get("name") + " for " + params.get("appName"));
  }

  public void checkStatus(String rcName, String serviceName) {
    ReplicationController rc = client.replicationControllers().inNamespace("default").withName(rcName).get();
    logger.info("Replication controller " + rcName + ": " + client.getMasterUrl()
        + rc.getMetadata().getSelfLink().substring(1));
    Service service = client.services().withName(serviceName).get();
    logger.info(
        "Service " + serviceName + ": " + client.getMasterUrl() + service.getMetadata().getSelfLink().substring(1));
  }

  public void cleanup() {
    if (client.services().list().getItems() != null) {
      client.services().delete();
      logger.info("Deleted existing services");
    }
    if (client.replicationControllers().list().getItems() != null) {
      client.replicationControllers().delete();
      logger.info("Deleted existing replication controllers");
    }
  }
}

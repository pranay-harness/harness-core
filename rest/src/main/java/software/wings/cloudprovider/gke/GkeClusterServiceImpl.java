package software.wings.cloudprovider.gke;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.Operation;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.service.impl.KubernetesHelperService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by bzane on 2/21/17.
 */
@Singleton
public class GkeClusterServiceImpl implements GkeClusterService {
  private static final int SLEEP_INTERVAL_S = 5;
  private static final int SLEEP_INTERVAL_MS = SLEEP_INTERVAL_S * 1000;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();

  @Override
  public KubernetesConfig createCluster(Map<String, String> params) {
    Container gkeContainerService = kubernetesHelperService.getGkeContainerService(params.get("appName"));

    // See if the cluster already exists
    try {
      Cluster cluster = gkeContainerService.projects()
                            .zones()
                            .clusters()
                            .get(params.get("projectId"), params.get("zone"), params.get("name"))
                            .execute();
      logger.info(String.format("Cluster %s already exists in zone %s for project %s", params.get("name"),
          params.get("zone"), params.get("projectId")));
      return configFromCluster(cluster);
    } catch (IOException e) {
      if (e instanceof GoogleJsonResponseException
          && ((GoogleJsonResponseException) e).getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        logger.info(String.format("Cluster %s does not exist in zone %s for project %s", params.get("name"),
            params.get("zone"), params.get("projectId")));
      } else {
        logger.error(String.format("Error getting cluster %s in zone %s for project %s", params.get("name"),
                         params.get("zone"), params.get("projectId")),
            e);
      }
    }

    // Cluster doesn't exist. Create it.
    try {
      CreateClusterRequest content = new CreateClusterRequest().setCluster(
          new Cluster()
              .setName(params.get("name"))
              .setInitialNodeCount(Integer.valueOf(params.get("nodeCount")))
              .setMasterAuth(
                  new MasterAuth().setUsername(params.get("masterUser")).setPassword(params.get("masterPwd"))));
      Operation createOperation = gkeContainerService.projects()
                                      .zones()
                                      .clusters()
                                      .create(params.get("projectId"), params.get("zone"), content)
                                      .execute();
      waitForOperationToComplete(
          createOperation, gkeContainerService, params.get("projectId"), params.get("zone"), "Provisioning");
      Cluster cluster = gkeContainerService.projects()
                            .zones()
                            .clusters()
                            .get(params.get("projectId"), params.get("zone"), params.get("name"))
                            .execute();
      logger.info("Cluster status: " + cluster.getStatus());
      logger.info("Master endpoint: " + cluster.getEndpoint());
      return configFromCluster(cluster);
    } catch (IOException e) {
      logger.error(String.format("Error creating cluster %s in zone %s for project %s", params.get("name"),
                       params.get("zone"), params.get("projectId")),
          e);
    }
    return null;
  }

  private KubernetesConfig configFromCluster(Cluster cluster) {
    return KubernetesConfig.Builder.aKubernetesConfig()
        .withApiServerUrl("https://" + cluster.getEndpoint() + "/")
        .withUsername(cluster.getMasterAuth().getUsername())
        .withPassword(cluster.getMasterAuth().getPassword())
        .build();
  }

  @Override
  public void deleteCluster(Map<String, String> params) {
    Container gkeContainerService = kubernetesHelperService.getGkeContainerService(params.get("appName"));

    Operation response = null;
    try {
      response = gkeContainerService.projects()
                     .zones()
                     .clusters()
                     .delete(params.get("projectId"), params.get("zone"), params.get("name"))
                     .execute();
      waitForOperationToComplete(
          response, gkeContainerService, params.get("projectId"), params.get("zone"), "Deleting cluster");
    } catch (IOException e) {
      logger.error(String.format("Error deleting cluster %s in zone %s for project %s", params.get("name"),
                       params.get("zone"), params.get("projectId")),
          e);
    }
  }

  private void waitForOperationToComplete(
      Operation operation, Container gkeContainerService, String projectId, String zone, String operationLogMessage) {
    logger.info(operationLogMessage + "...");
    int i = 0;
    while (operation.getStatus().equals("RUNNING")) {
      try {
        Thread.sleep(SLEEP_INTERVAL_MS);
        operation =
            gkeContainerService.projects().zones().operations().get(projectId, zone, operation.getName()).execute();
      } catch (IOException e) {
        logger.error("Error checking operation status", e);
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for operation to complete", e);
      }
      i += SLEEP_INTERVAL_S;
      logger.info(operationLogMessage + "... " + i);
    }
    logger.info(operationLogMessage + ": " + operation.getStatus());
  }

  @Override
  public KubernetesConfig getCluster(Map<String, String> params) {
    Container gkeContainerService = kubernetesHelperService.getGkeContainerService(params.get("appName"));

    try {
      Cluster cluster = gkeContainerService.projects()
                            .zones()
                            .clusters()
                            .get(params.get("projectId"), params.get("zone"), params.get("name"))
                            .execute();
      logger.info(String.format("Found cluster %s in zone %s for project %s", params.get("name"), params.get("zone"),
          params.get("projectId")));
      logger.info("Cluster status: " + cluster.getStatus());
      logger.info("Master endpoint: " + cluster.getEndpoint());
      return configFromCluster(cluster);
    } catch (IOException e) {
      if (e instanceof GoogleJsonResponseException
          && ((GoogleJsonResponseException) e).getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        logger.info(String.format("Cluster %s does not exist in zone %s for project %s", params.get("name"),
            params.get("zone"), params.get("projectId")));
      } else {
        logger.error(String.format("Error getting cluster %s in zone %s for project %s", params.get("name"),
                         params.get("zone"), params.get("projectId")),
            e);
      }
    }
    return null;
  }

  @Override
  public List<String> listClusters(Map<String, String> params) {
    return null;
  }

  @Override
  public void setNodePoolAutoscaling(boolean enabled, int min, int max) {}

  @Override
  public boolean getNodePoolAutoscaling() {
    return false;
  }
}

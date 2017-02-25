package software.wings.cloudprovider.gke;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.ClusterUpdate;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.ListClustersResponse;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.NodePool;
import com.google.api.services.container.model.NodePoolAutoscaling;
import com.google.api.services.container.model.Operation;
import com.google.api.services.container.model.UpdateClusterRequest;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by bzane on 2/21/17.
 */
@Singleton
public class GkeClusterServiceImpl implements GkeClusterService {
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
      logNotFoundOrError(e, params, "getting");
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
      String operationStatus = waitForOperationToComplete(
          createOperation, gkeContainerService, params.get("projectId"), params.get("zone"), "Provisioning");
      if (operationStatus.equals("DONE")) {
        Cluster cluster = gkeContainerService.projects()
                              .zones()
                              .clusters()
                              .get(params.get("projectId"), params.get("zone"), params.get("name"))
                              .execute();
        logger.info("Cluster status: " + cluster.getStatus());
        logger.info("Master endpoint: " + cluster.getEndpoint());
        return configFromCluster(cluster);
      }
    } catch (IOException e) {
      logNotFoundOrError(e, params, "creating");
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
  public boolean deleteCluster(Map<String, String> params) {
    Container gkeContainerService = kubernetesHelperService.getGkeContainerService(params.get("appName"));
    try {
      Operation deleteOperation = gkeContainerService.projects()
                                      .zones()
                                      .clusters()
                                      .delete(params.get("projectId"), params.get("zone"), params.get("name"))
                                      .execute();
      String operationStatus = waitForOperationToComplete(
          deleteOperation, gkeContainerService, params.get("projectId"), params.get("zone"), "Deleting cluster");
      if (operationStatus.equals("DONE")) {
        return true;
      }
    } catch (IOException e) {
      logNotFoundOrError(e, params, "deleting");
    }
    return false;
  }

  private String waitForOperationToComplete(
      Operation operation, Container gkeContainerService, String projectId, String zone, String operationLogMessage) {
    logger.info(operationLogMessage + "...");
    int i = 0;
    while (operation.getStatus().equals("RUNNING")) {
      try {
        Misc.quietSleep(kubernetesHelperService.getSleepIntervalMs());
        operation =
            gkeContainerService.projects().zones().operations().get(projectId, zone, operation.getName()).execute();
      } catch (IOException e) {
        logger.error("Error checking operation status", e);
        break;
      }
      i += kubernetesHelperService.getSleepIntervalMs() / 1000;
      logger.info(operationLogMessage + "... " + i);
    }
    logger.info(operationLogMessage + ": " + operation.getStatus());
    return operation.getStatus();
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
      logNotFoundOrError(e, params, "getting");
    }
    return null;
  }

  @Override
  public List<String> listClusters(Map<String, String> params) {
    Container gkeContainerService = kubernetesHelperService.getGkeContainerService(params.get("appName"));
    try {
      ListClustersResponse response =
          gkeContainerService.projects().zones().clusters().list(params.get("projectId"), params.get("zone")).execute();
      return response.getClusters().stream().map(Cluster::getName).collect(Collectors.toList());
    } catch (IOException e) {
      logger.error(String.format(
          "Error listing clusters for project %s in zone %s", params.get("projectId"), params.get("zone")));
    }
    return null;
  }

  @Override
  public boolean setNodePoolAutoscaling(boolean enabled, int min, int max, Map<String, String> params) {
    Container gkeContainerService = kubernetesHelperService.getGkeContainerService(params.get("appName"));
    try {
      ClusterUpdate clusterUpdate = new ClusterUpdate();
      if (params.containsKey("nodePoolId")) {
        clusterUpdate.setDesiredNodePoolId(params.get("nodePoolId"));
      }
      UpdateClusterRequest update = new UpdateClusterRequest().setUpdate(clusterUpdate.setDesiredNodePoolAutoscaling(
          new NodePoolAutoscaling().setEnabled(enabled).setMinNodeCount(min).setMaxNodeCount(max)));
      Operation updateOperation = gkeContainerService.projects()
                                      .zones()
                                      .clusters()
                                      .update(params.get("projectId"), params.get("zone"), params.get("name"), update)
                                      .execute();
      String operationStatus = waitForOperationToComplete(
          updateOperation, gkeContainerService, params.get("projectId"), params.get("zone"), "Updating cluster");
      if (operationStatus.equals("DONE")) {
        return true;
      }
    } catch (IOException e) {
      logNotFoundOrError(e, params, "updating");
    }
    return false;
  }

  private void logNotFoundOrError(IOException e, Map<String, String> params, String actionVerb) {
    if (e instanceof GoogleJsonResponseException
        && ((GoogleJsonResponseException) e).getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
      logger.warn(String.format("Cluster %s does not exist in zone %s for project %s", params.get("name"),
          params.get("zone"), params.get("projectId")));
    } else {
      logger.error(String.format("Error %s cluster %s in zone %s for project %s", actionVerb, params.get("name"),
                       params.get("zone"), params.get("projectId")),
          e);
    }
  }

  @Override
  public NodePoolAutoscaling getNodePoolAutoscaling(Map<String, String> params) {
    Container gkeContainerService = kubernetesHelperService.getGkeContainerService(params.get("appName"));
    try {
      Cluster cluster = gkeContainerService.projects()
                            .zones()
                            .clusters()
                            .get(params.get("projectId"), params.get("zone"), params.get("name"))
                            .execute();
      if (params.containsKey("nodePoolId")) {
        String nodePoolId = params.get("nodePoolId");
        for (NodePool nodePool : cluster.getNodePools()) {
          if (nodePool.getName().equals(nodePoolId)) {
            return nodePool.getAutoscaling();
          }
        }
      } else {
        return Iterables.getOnlyElement(cluster.getNodePools()).getAutoscaling();
      }
    } catch (IOException e) {
      logNotFoundOrError(e, params, "getting");
    }
    return null;
  }
}

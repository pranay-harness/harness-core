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
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.exception.WingsException;
import software.wings.service.impl.GcpHelperService;
import software.wings.utils.Misc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.service.impl.GcpHelperService.ALL_ZONES;
import static software.wings.service.impl.GcpHelperService.ZONE_DELIMITER;

/**
 * Created by bzane on 2/21/17
 */
@Singleton
public class GkeClusterServiceImpl implements GkeClusterService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private GcpHelperService gcpHelperService = new GcpHelperService();

  @Override
  public KubernetesConfig createCluster(
      SettingAttribute computeProviderSetting, String zoneClusterName, Map<String, String> params) {
    String credentials = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(credentials);
    String projectId = getProjectIdFromCredentials(credentials);
    String[] zoneCluster = zoneClusterName.split(ZONE_DELIMITER);
    String zone = zoneCluster[0];
    String clusterName = zoneCluster[1];
    // See if the cluster already exists
    try {
      Cluster cluster = gkeContainerService.projects().zones().clusters().get(projectId, zone, clusterName).execute();
      logger.info("Cluster {} already exists in zone {} for project {}", clusterName, zone, projectId);
      return configFromCluster(cluster);
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, zone, clusterName, "getting");
    }

    // Cluster doesn't exist. Create it.
    try {
      CreateClusterRequest content = new CreateClusterRequest().setCluster(
          new Cluster()
              .setName(clusterName)
              .setInitialNodeCount(Integer.valueOf(params.get("nodeCount")))
              .setMasterAuth(
                  new MasterAuth().setUsername(params.get("masterUser")).setPassword(params.get("masterPwd"))));
      Operation createOperation =
          gkeContainerService.projects().zones().clusters().create(projectId, zone, content).execute();
      String operationStatus =
          waitForOperationToComplete(createOperation, gkeContainerService, projectId, zone, "Provisioning");
      if (operationStatus.equals("DONE")) {
        Cluster cluster = gkeContainerService.projects().zones().clusters().get(projectId, zone, clusterName).execute();
        logger.info("Cluster status: " + cluster.getStatus());
        logger.info("Master endpoint: " + cluster.getEndpoint());
        return configFromCluster(cluster);
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, zone, clusterName, "creating");
    }
    return null;
  }

  private KubernetesConfig configFromCluster(Cluster cluster) {
    return KubernetesConfig.Builder.aKubernetesConfig()
        .withMasterUrl("https://" + cluster.getEndpoint() + "/")
        .withUsername(cluster.getMasterAuth().getUsername())
        .withPassword(cluster.getMasterAuth().getPassword())
        .build();
  }

  private String validateAndGetCredentials(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof GcpConfig)) {
      throw new WingsException(INVALID_ARGUMENT, "message", "InvalidConfiguration");
    }
    return ((GcpConfig) computeProviderSetting.getValue()).getServiceAccountKeyFileContent();
  }

  @Override
  public boolean deleteCluster(SettingAttribute computeProviderSetting, String zoneClusterName) {
    String credentials = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(credentials);
    String projectId = getProjectIdFromCredentials(credentials);
    String[] zoneCluster = zoneClusterName.split(ZONE_DELIMITER);
    String zone = zoneCluster[0];
    String clusterName = zoneCluster[1];
    try {
      Operation deleteOperation =
          gkeContainerService.projects().zones().clusters().delete(projectId, zone, clusterName).execute();
      String operationStatus =
          waitForOperationToComplete(deleteOperation, gkeContainerService, projectId, zone, "Deleting cluster");
      if (operationStatus.equals("DONE")) {
        return true;
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, zone, clusterName, "deleting");
    }
    return false;
  }

  private String waitForOperationToComplete(
      Operation operation, Container gkeContainerService, String projectId, String zone, String operationLogMessage) {
    logger.info(operationLogMessage + "...");
    int i = 0;
    while (operation.getStatus().equals("RUNNING")) {
      try {
        Misc.quietSleep(gcpHelperService.getSleepIntervalMs());
        operation =
            gkeContainerService.projects().zones().operations().get(projectId, zone, operation.getName()).execute();
      } catch (IOException e) {
        logger.error("Error checking operation status", e);
        break;
      }
      i += gcpHelperService.getSleepIntervalMs() / 1000;
      logger.info(operationLogMessage + "... " + i);
    }
    logger.info(operationLogMessage + ": " + operation.getStatus());
    return operation.getStatus();
  }

  @Override
  public KubernetesConfig getCluster(SettingAttribute computeProviderSetting, String zoneClusterName) {
    String credentials = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(credentials);
    String projectId = getProjectIdFromCredentials(credentials);
    String[] zoneCluster = zoneClusterName.split(ZONE_DELIMITER);
    String zone = zoneCluster[0];
    String clusterName = zoneCluster[1];
    try {
      Cluster cluster = gkeContainerService.projects().zones().clusters().get(projectId, zone, clusterName).execute();
      logger.info("Found cluster {} in zone {} for project {}", clusterName, zone, projectId);
      logger.info("Cluster status: " + cluster.getStatus());
      logger.info("Master endpoint: " + cluster.getEndpoint());
      return configFromCluster(cluster);
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, zone, clusterName, "getting");
    }
    return null;
  }

  @Override
  public List<String> listClusters(SettingAttribute computeProviderSetting) {
    String credentials = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(credentials);
    String projectId = getProjectIdFromCredentials(credentials);
    try {
      ListClustersResponse response =
          gkeContainerService.projects().zones().clusters().list(projectId, ALL_ZONES).execute();
      return response.getClusters()
          .stream()
          .map(cluster -> cluster.getZone() + ZONE_DELIMITER + cluster.getName())
          .collect(Collectors.toList());
    } catch (IOException e) {
      logger.error("Error listing clusters for project {}", projectId);
    }
    return null;
  }

  private String getProjectIdFromCredentials(String credentials) {
    return (String) ((DBObject) JSON.parse(credentials)).get("project_id");
  }

  @Override
  public boolean setNodePoolAutoscaling(SettingAttribute computeProviderSetting, String zoneClusterName,
      @Nullable String nodePoolId, boolean enabled, int min, int max) {
    String credentials = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(credentials);
    String projectId = getProjectIdFromCredentials(credentials);
    String[] zoneCluster = zoneClusterName.split(ZONE_DELIMITER);
    String zone = zoneCluster[0];
    String clusterName = zoneCluster[1];
    try {
      ClusterUpdate clusterUpdate = new ClusterUpdate();
      if (!isNullOrEmpty(nodePoolId)) {
        clusterUpdate.setDesiredNodePoolId(nodePoolId);
      }
      UpdateClusterRequest update = new UpdateClusterRequest().setUpdate(clusterUpdate.setDesiredNodePoolAutoscaling(
          new NodePoolAutoscaling().setEnabled(enabled).setMinNodeCount(min).setMaxNodeCount(max)));
      Operation updateOperation =
          gkeContainerService.projects().zones().clusters().update(projectId, zone, clusterName, update).execute();
      String operationStatus =
          waitForOperationToComplete(updateOperation, gkeContainerService, projectId, zone, "Updating cluster");
      if (operationStatus.equals("DONE")) {
        return true;
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, zone, clusterName, "updating");
    }
    return false;
  }

  private void logNotFoundOrError(IOException e, String projectId, String zone, String clusterName, String actionVerb) {
    if (e instanceof GoogleJsonResponseException
        && ((GoogleJsonResponseException) e).getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
      logger.warn("Cluster {} does not exist in zone {} for project {}", clusterName, zone, projectId);
    } else {
      logger.error("Error {} cluster {} in zone {} for project {}", actionVerb, clusterName, zone, projectId, e);
    }
  }

  @Override
  public NodePoolAutoscaling getNodePoolAutoscaling(
      SettingAttribute computeProviderSetting, String zoneClusterName, @Nullable String nodePoolId) {
    String credentials = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(credentials);
    String projectId = getProjectIdFromCredentials(credentials);
    String[] zoneCluster = zoneClusterName.split(ZONE_DELIMITER);
    String zone = zoneCluster[0];
    String clusterName = zoneCluster[1];
    try {
      Cluster cluster = gkeContainerService.projects().zones().clusters().get(projectId, zone, clusterName).execute();
      if (!isNullOrEmpty(nodePoolId)) {
        for (NodePool nodePool : cluster.getNodePools()) {
          if (nodePool.getName().equals(nodePoolId)) {
            return nodePool.getAutoscaling();
          }
        }
      } else {
        return Iterables.getOnlyElement(cluster.getNodePools()).getAutoscaling();
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, zone, clusterName, "getting");
    }
    return null;
  }
}

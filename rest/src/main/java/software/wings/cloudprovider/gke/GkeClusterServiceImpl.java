package software.wings.cloudprovider.gke;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.service.impl.GcpHelperService.ALL_LOCATIONS;
import static software.wings.service.impl.GcpHelperService.LOCATION_DELIMITER;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.ClusterUpdate;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.ListClustersResponse;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.NodeConfig;
import com.google.api.services.container.model.NodePool;
import com.google.api.services.container.model.NodePoolAutoscaling;
import com.google.api.services.container.model.Operation;
import com.google.api.services.container.model.UpdateClusterRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.KubernetesConfig.KubernetesConfigBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GcpHelperService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Created by bzane on 2/21/17
 */
@Singleton
public class GkeClusterServiceImpl implements GkeClusterService {
  private static final Logger logger = LoggerFactory.getLogger(GkeClusterServiceImpl.class);

  @Inject private GcpHelperService gcpHelperService = new GcpHelperService();
  @Inject private TimeLimiter timeLimiter;

  @Override
  public KubernetesConfig createCluster(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String locationClusterName, String namespace,
      Map<String, String> params) {
    GcpConfig gcpConfig = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(gcpConfig, encryptedDataDetails);
    String projectId = getProjectIdFromCredentials(gcpConfig.getServiceAccountKeyFileContent());
    String[] locationCluster = locationClusterName.split(LOCATION_DELIMITER);
    String location = locationCluster[0];
    String clusterName = locationCluster[1];
    // See if the cluster already exists
    try {
      Cluster cluster = gkeContainerService.projects()
                            .locations()
                            .clusters()
                            .get("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
                            .execute();
      logger.info("Cluster already exists");
      logger.debug("Cluster {}, location {}, project {}", clusterName, location, projectId);
      return configFromCluster(cluster, namespace);
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, location, clusterName, "getting");
    }

    // Cluster doesn't exist. Create it.
    try {
      CreateClusterRequest content = new CreateClusterRequest().setCluster(
          new Cluster()
              .setName(clusterName)
              .setNodeConfig(new NodeConfig().setMachineType(params.get("machineType")))
              .setInitialNodeCount(Integer.valueOf(params.get("nodeCount")))
              .setMasterAuth(
                  new MasterAuth().setUsername(params.get("masterUser")).setPassword(params.get("masterPwd"))));
      Operation createOperation =
          gkeContainerService.projects()
              .locations()
              .clusters()
              .create("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName, content)
              .execute();
      String operationStatus =
          waitForOperationToComplete(createOperation, gkeContainerService, projectId, location, "Provisioning");
      if (operationStatus.equals("DONE")) {
        Cluster cluster = gkeContainerService.projects()
                              .locations()
                              .clusters()
                              .get("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
                              .execute();
        logger.info("Cluster status: {}", cluster.getStatus());
        logger.debug("Master endpoint: {}", cluster.getEndpoint());
        return configFromCluster(cluster, namespace);
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, location, clusterName, "creating");
    }
    return null;
  }

  private KubernetesConfig configFromCluster(Cluster cluster, String namespace) {
    MasterAuth masterAuth = cluster.getMasterAuth();
    KubernetesConfigBuilder kubernetesConfigBuilder = KubernetesConfig.builder()
                                                          .masterUrl("https://" + cluster.getEndpoint() + "/")
                                                          .namespace(isNotBlank(namespace) ? namespace : "default");
    if (masterAuth.getUsername() != null) {
      kubernetesConfigBuilder.username(masterAuth.getUsername());
    }
    if (masterAuth.getPassword() != null) {
      kubernetesConfigBuilder.password(masterAuth.getPassword().toCharArray());
    }
    if (masterAuth.getClusterCaCertificate() != null) {
      kubernetesConfigBuilder.caCert(masterAuth.getClusterCaCertificate().toCharArray());
    }
    if (masterAuth.getClientCertificate() != null) {
      kubernetesConfigBuilder.clientCert(masterAuth.getClientCertificate().toCharArray());
    }
    if (masterAuth.getClientKey() != null) {
      kubernetesConfigBuilder.clientKey(masterAuth.getClientKey().toCharArray());
    }
    KubernetesConfig kubernetesConfig = kubernetesConfigBuilder.build();
    kubernetesConfig.setDecrypted(true);
    return kubernetesConfig;
  }

  private GcpConfig validateAndGetCredentials(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof GcpConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }
    return (GcpConfig) computeProviderSetting.getValue();
  }

  @Override
  public boolean deleteCluster(SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      String locationClusterName) {
    GcpConfig gcpConfig = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(gcpConfig, encryptedDataDetails);
    String projectId = getProjectIdFromCredentials(gcpConfig.getServiceAccountKeyFileContent());
    String[] locationCluster = locationClusterName.split(LOCATION_DELIMITER);
    String location = locationCluster[0];
    String clusterName = locationCluster[1];
    try {
      Operation deleteOperation =
          gkeContainerService.projects()
              .locations()
              .clusters()
              .delete("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
              .execute();
      String operationStatus =
          waitForOperationToComplete(deleteOperation, gkeContainerService, projectId, location, "Deleting cluster");
      if (operationStatus.equals("DONE")) {
        return true;
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, location, clusterName, "deleting");
    }
    return false;
  }

  private String waitForOperationToComplete(Operation operation, Container gkeContainerService, String projectId,
      String location, String operationLogMessage) {
    logger.info(operationLogMessage + "...");
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          String status =
              gkeContainerService.projects()
                  .locations()
                  .operations()
                  .get("projects/" + projectId + "/locations/" + location + "/operations/" + operation.getName())
                  .execute()
                  .getStatus();
          if (!status.equals("RUNNING")) {
            logger.info(operationLogMessage + ": " + status);
            return status;
          }
          sleep(ofSeconds(gcpHelperService.getSleepIntervalSecs()));
        }
      }, gcpHelperService.getTimeoutMins(), TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      logger.error("Timed out checking operation status");
      return "UNKNOWN";
    } catch (Exception e) {
      logger.error("Error checking operation status", e);
      return "UNKNOWN";
    }
  }

  @Override
  public KubernetesConfig getCluster(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String locationClusterName, String namespace) {
    GcpConfig gcpConfig = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(gcpConfig, encryptedDataDetails);
    String projectId = getProjectIdFromCredentials(gcpConfig.getServiceAccountKeyFileContent());
    String[] locationCluster = locationClusterName.split(LOCATION_DELIMITER);
    String location = locationCluster[0];
    String clusterName = locationCluster[1];
    try {
      Cluster cluster = gkeContainerService.projects()
                            .locations()
                            .clusters()
                            .get("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
                            .execute();
      logger.debug("Found cluster {} in location {} for project {}", clusterName, location, projectId);
      logger.info("Cluster status: {}", cluster.getStatus());
      logger.debug("Master endpoint: {}", cluster.getEndpoint());
      return configFromCluster(cluster, namespace);
    } catch (IOException exception) {
      logNotFoundOrError(exception, projectId, location, clusterName, "getting");
      throw new InvalidRequestException(
          format("Error getting cluster %s in location %s for project %s", clusterName, location, projectId),
          exception);
    }
  }

  @Override
  public List<String> listClusters(
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails) {
    GcpConfig gcpConfig = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(gcpConfig, encryptedDataDetails);
    String projectId = getProjectIdFromCredentials(gcpConfig.getServiceAccountKeyFileContent());
    try {
      ListClustersResponse response = gkeContainerService.projects()
                                          .locations()
                                          .clusters()
                                          .list("projects/" + projectId + "/locations/" + ALL_LOCATIONS)
                                          .execute();
      List<Cluster> clusters = response.getClusters();
      return clusters != null ? clusters.stream()
                                    .map(cluster -> cluster.getZone() + LOCATION_DELIMITER + cluster.getName())
                                    .collect(toList())
                              : ImmutableList.of();
    } catch (IOException e) {
      logger.error("Error listing clusters for project " + projectId, e);
    }
    return null;
  }

  private String getProjectIdFromCredentials(char[] credentials) {
    return (String) ((DBObject) JSON.parse(new String(credentials))).get("project_id");
  }

  @Override
  public boolean setNodePoolAutoscaling(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String locationClusterName, @Nullable String nodePoolId,
      boolean enabled, int min, int max) {
    GcpConfig gcpConfig = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(gcpConfig, encryptedDataDetails);
    String projectId = getProjectIdFromCredentials(gcpConfig.getServiceAccountKeyFileContent());
    String[] locationCluster = locationClusterName.split(LOCATION_DELIMITER);
    String location = locationCluster[0];
    String clusterName = locationCluster[1];
    try {
      ClusterUpdate clusterUpdate = new ClusterUpdate();
      if (isNotBlank(nodePoolId)) {
        clusterUpdate.setDesiredNodePoolId(nodePoolId);
      }
      UpdateClusterRequest update = new UpdateClusterRequest().setUpdate(clusterUpdate.setDesiredNodePoolAutoscaling(
          new NodePoolAutoscaling().setEnabled(enabled).setMinNodeCount(min).setMaxNodeCount(max)));
      Operation updateOperation =
          gkeContainerService.projects()
              .locations()
              .clusters()
              .update("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName, update)
              .execute();
      String operationStatus =
          waitForOperationToComplete(updateOperation, gkeContainerService, projectId, location, "Updating cluster");
      if (operationStatus.equals("DONE")) {
        return true;
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, location, clusterName, "updating");
    }
    return false;
  }

  private void logNotFoundOrError(
      IOException e, String projectId, String location, String clusterName, String actionVerb) {
    if (e instanceof GoogleJsonResponseException
        && ((GoogleJsonResponseException) e).getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
      logger.warn(
          format("Cluster %s does not exist in location %s for project %s", clusterName, location, projectId), e);
    } else {
      logger.error(
          format("Error %s cluster %s in location %s for project %s", actionVerb, clusterName, location, projectId), e);
    }
  }

  @Override
  public NodePoolAutoscaling getNodePoolAutoscaling(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String locationClusterName, @Nullable String nodePoolId) {
    GcpConfig gcpConfig = validateAndGetCredentials(computeProviderSetting);
    Container gkeContainerService = gcpHelperService.getGkeContainerService(gcpConfig, encryptedDataDetails);
    String projectId = getProjectIdFromCredentials(gcpConfig.getServiceAccountKeyFileContent());
    String[] locationCluster = locationClusterName.split(LOCATION_DELIMITER);
    String location = locationCluster[0];
    String clusterName = locationCluster[1];
    try {
      Cluster cluster = gkeContainerService.projects()
                            .locations()
                            .clusters()
                            .get("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
                            .execute();
      if (isNotBlank(nodePoolId)) {
        for (NodePool nodePool : cluster.getNodePools()) {
          if (nodePool.getName().equals(nodePoolId)) {
            return nodePool.getAutoscaling();
          }
        }
      } else {
        return Iterables.getOnlyElement(cluster.getNodePools()).getAutoscaling();
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, location, clusterName, "getting");
    }
    return null;
  }
}

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
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.Operation;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by bzane on 2/21/17.
 */
@Singleton
public class GkeClusterServiceImpl implements GkeClusterService {
  private static final int SLEEP_INTERVAL = 5 * 1000;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   *
   * @param appName
   */
  private Container getGkeContainerService(String appName) {
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
  public KubernetesConfig createCluster(Map<String, String> params) {
    Container gkeContainerService = getGkeContainerService(params.get("appName"));

    Cluster cluster = null;
    try {
      cluster = gkeContainerService.projects()
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
        response = gkeContainerService.projects()
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
        try {
          Thread.sleep(SLEEP_INTERVAL);
          response = gkeContainerService.projects()
                         .zones()
                         .operations()
                         .get(params.get("projectId"), params.get("zone"), response.getName())
                         .execute();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        i += 5;
        logger.info("Provisioning... " + i);
      }
      logger.info("Provisioning: " + response.getStatus());

      try {
        cluster = gkeContainerService.projects()
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

    return KubernetesConfig.Builder.aKubernetesConfig()
        .withApiServerUrl("https://" + cluster.getEndpoint() + "/")
        .withUsername(params.get("masterUser"))
        .withPassword(params.get("masterPwd"))
        .build();
  }

  @Override
  public void deleteCluster(Map<String, String> params) {
    Container gkeContainerService = getGkeContainerService(params.get("appName"));

    Operation response = null;
    try {
      response = gkeContainerService.projects()
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
      try {
        Thread.sleep(SLEEP_INTERVAL);
        response = gkeContainerService.projects()
                       .zones()
                       .operations()
                       .get(params.get("projectId"), params.get("zone"), response.getName())
                       .execute();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i += 5;
      logger.info("Deleting cluster... " + i);
    }
    logger.info("Delete cluster: " + response.getStatus());
  }

  @Override
  public KubernetesConfig getCluster(Map<String, String> params) {
    Container gkeContainerService = getGkeContainerService(params.get("appName"));

    Cluster cluster = null;
    try {
      cluster = gkeContainerService.projects()
                    .zones()
                    .clusters()
                    .get(params.get("projectId"), params.get("zone"), params.get("name"))
                    .execute();
      logger.info("Found cluster " + params.get("name") + " in zone " + params.get("zone") + " for project "
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
        return null;
      }
    }

    logger.info("Cluster status: " + cluster.getStatus());
    logger.info("Master endpoint: " + cluster.getEndpoint());

    return KubernetesConfig.Builder.aKubernetesConfig()
        .withApiServerUrl("https://" + cluster.getEndpoint() + "/")
        .withUsername(params.get("masterUser"))
        .withPassword(params.get("masterPwd"))
        .build();
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

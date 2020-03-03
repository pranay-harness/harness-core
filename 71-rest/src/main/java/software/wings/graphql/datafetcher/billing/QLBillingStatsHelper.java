package software.wings.graphql.datafetcher.billing;

import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.service.intfc.SettingsService;

@Singleton
@Slf4j
public class QLBillingStatsHelper {
  @Inject WingsPersistence wingsPersistence;
  @Inject ClusterRecordService clusterRecordService;
  @Inject SettingsService settingsService;

  public String getEntityName(BillingDataMetaDataFields field, String entityId) {
    switch (field) {
      case APPID:
        return getApplicationName(entityId);
      case ENVID:
        return getEnvironmentName(entityId);
      case SERVICEID:
        return getServiceName(entityId);
      case CLUSTERID:
        return getClusterName(entityId);
      case CLOUDPROVIDERID:
        return getCloudProviderName(entityId);
      case REGION:
      case CLOUDSERVICENAME:
      case TASKID:
      case LAUNCHTYPE:
      case WORKLOADNAME:
      case WORKLOADTYPE:
      case NAMESPACE:
      case CLUSTERNAME:
        return entityId;
      default:
        throw new InvalidRequestException("Invalid EntityType " + field);
    }
  }

  private String getApplicationName(String entityId) {
    try {
      Application application = wingsPersistence.get(Application.class, entityId);
      if (application != null) {
        return application.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      return entityId;
    }
  }

  private String getEnvironmentName(String entityId) {
    try {
      Environment environment = wingsPersistence.get(Environment.class, entityId);
      if (environment != null) {
        return environment.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      return entityId;
    }
  }

  private String getServiceName(String entityId) {
    try {
      Service service = wingsPersistence.get(Service.class, entityId);
      if (service != null) {
        return service.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      return entityId;
    }
  }

  private String getClusterName(String entityId) {
    try {
      Cluster cluster = clusterRecordService.get(entityId).getCluster();
      if (cluster != null) {
        if (cluster.getClusterType().equals(AWS_ECS)) {
          EcsCluster ecsCluster = (EcsCluster) cluster;
          if (null != ecsCluster.getClusterName()) {
            return ecsCluster.getClusterName();
          } else {
            return entityId;
          }
        } else if (cluster.getClusterType().equals(DIRECT_KUBERNETES)) {
          DirectKubernetesCluster kubernetesCluster = (DirectKubernetesCluster) cluster;
          String clusterName = kubernetesCluster.getClusterName();
          if (null == clusterName || clusterName.equals("")) {
            SettingAttribute settingAttribute = settingsService.get(kubernetesCluster.getCloudProviderId());
            clusterName = settingAttribute.getName();
          }
          return clusterName;
        } else {
          return entityId;
        }
      } else {
        return entityId;
      }
    } catch (Exception e) {
      return entityId;
    }
  }

  public String getCloudProviderName(String entityId) {
    try {
      SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, entityId);
      if (settingAttribute != null) {
        return settingAttribute.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      return entityId;
    }
  }
}

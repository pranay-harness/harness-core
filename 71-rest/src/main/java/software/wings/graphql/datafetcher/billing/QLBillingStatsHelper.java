package software.wings.graphql.datafetcher.billing;

import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;

import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.InstanceDataServiceImpl;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.ccm.health.CEClusterDao;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CECluster;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.service.intfc.SettingsService;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class QLBillingStatsHelper {
  @Inject WingsPersistence wingsPersistence;
  @Inject ClusterRecordService clusterRecordService;
  @Inject CEClusterDao ceClusterDao;
  @Inject SettingsService settingsService;
  @Inject InstanceDataServiceImpl instanceDataService;
  private static final long CACHE_SIZE = 10000;

  private LoadingCache<CacheKey, String> entityIdToNameCache =
      Caffeine.newBuilder().maximumSize(CACHE_SIZE).build(key -> fetchEntityName(key.getField(), key.getEntityId()));

  private LoadingCache<CacheKey, String> entityIdWithAccountToNameCache =
      Caffeine.newBuilder()
          .maximumSize(CACHE_SIZE)
          .build(key -> fetchEntityNameWithAccountFilter(key.getField(), key.getEntityId(), key.getAccountId()));

  @Value
  protected static class CacheKey {
    private String entityId;
    private String accountId;
    private BillingDataMetaDataFields field;
  }

  public String getEntityName(BillingDataMetaDataFields field, String entityId, ResultSet resultSet)
      throws SQLException {
    switch (field) {
      case INSTANCEID:
        return resultSet.getString(BillingDataMetaDataFields.INSTANCENAME.getFieldName());
      default:
        return getEntityName(field, entityId);
    }
  }

  public String getEntityName(BillingDataMetaDataFields field, String entityId) {
    switch (field) {
      case APPID:
      case ENVID:
      case SERVICEID:
      case CLUSTERID:
      case CLOUDPROVIDERID:
        CacheKey cacheKey = new CacheKey(entityId, "", field);
        return entityIdToNameCache.get(cacheKey);
      case INSTANCEID:
        return getInstanceName(entityId);
      case REGION:
      case CLOUDSERVICENAME:
      case TASKID:
      case LAUNCHTYPE:
      case WORKLOADNAME:
      case WORKLOADTYPE:
      case NAMESPACE:
      case CLUSTERNAME:
      case INSTANCENAME:
        return entityId;
      default:
        throw new InvalidRequestException("Invalid EntityType " + field);
    }
  }

  private String fetchEntityName(BillingDataMetaDataFields field, String entityId) {
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
      CECluster ceCluster = ceClusterDao.getCECluster(entityId);
      if (null != ceCluster) {
        return ceCluster.getClusterName();
      }
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
        }
      }
    } catch (Exception e) {
      return entityId;
    }
    return entityId;
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

  public String getInstanceName(String entityId) {
    try {
      InstanceData instanceData = instanceDataService.get(entityId);
      if (instanceData != null) {
        return instanceData.getInstanceName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      return entityId;
    }
  }

  public String getEntityName(BillingDataMetaDataFields field, String entityId, String accountId) {
    switch (field) {
      case APPID:
      case CLUSTERID:
        CacheKey cacheKey = new CacheKey(entityId, accountId, field);
        return entityIdWithAccountToNameCache.get(cacheKey);
      default:
        throw new InvalidRequestException("Invalid EntityType " + field);
    }
  }

  private String fetchEntityNameWithAccountFilter(BillingDataMetaDataFields field, String entityId, String accountId) {
    switch (field) {
      case APPID:
        return getApplicationNameWithAccountFilter(entityId, accountId);
      case CLUSTERID:
        return getClusterNameWithAccountFilter(entityId, accountId);
      default:
        throw new InvalidRequestException("Invalid EntityType " + field);
    }
  }

  private String getApplicationNameWithAccountFilter(String entityId, String accountId) {
    try {
      Application application = wingsPersistence.get(Application.class, entityId);
      if (application != null && application.getAccountId().equals(accountId)) {
        return application.getName();
      } else {
        return entityId;
      }
    } catch (Exception e) {
      return entityId;
    }
  }

  private String getClusterNameWithAccountFilter(String entityId, String accountId) {
    try {
      CECluster ceCluster = ceClusterDao.getCECluster(entityId);
      if (null != ceCluster && ceCluster.getAccountId().equals(accountId)) {
        return ceCluster.getClusterName();
      }
      ClusterRecord clusterRecord = clusterRecordService.get(entityId);
      Cluster cluster = clusterRecord.getCluster();
      if (cluster != null && clusterRecord.getAccountId().equals(accountId)) {
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
        }
      }
    } catch (Exception e) {
      return entityId;
    }
    return entityId;
  }
}

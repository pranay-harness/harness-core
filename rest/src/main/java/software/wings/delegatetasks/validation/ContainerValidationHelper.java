package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.network.Http.connectableHttpUrl;
import static software.wings.common.Constants.ALWAYS_TRUE_CRITERIA;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.File;
import java.util.List;

@Singleton
public class ContainerValidationHelper {
  private static final Logger logger = LoggerFactory.getLogger(ContainerValidationHelper.class);
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient AzureHelperService azureHelperService;
  @Inject @Transient private transient EncryptionService encryptionService;

  public boolean validateContainerServiceParams(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();

    // see if we can decrypt from this delegate
    if (Encryptable.class.isInstance(value) && isNotEmpty(containerServiceParams.getEncryptionDetails())) {
      try {
        encryptionService.decrypt((Encryptable) value, containerServiceParams.getEncryptionDetails());
      } catch (Exception e) {
        logger.info("failed to decrypt " + value, e);
        return false;
      }
    }

    boolean validated;
    if (value instanceof AwsConfig) {
      String region = containerServiceParams.getRegion();
      validated = region == null || AwsHelperService.isInAwsRegion(region) || isLocalDev();
    } else if (value instanceof KubernetesClusterConfig
        && ((KubernetesClusterConfig) value).isUseKubernetesDelegate()) {
      validated = ((KubernetesClusterConfig) value).getDelegateName().equals(System.getenv().get("DELEGATE_NAME"));
    } else {
      validated = connectableHttpUrl(getKubernetesMasterUrl(containerServiceParams));
    }

    return validated;
  }

  private static boolean isLocalDev() {
    return !new File("start.sh").exists();
  }

  public String getCriteria(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof AwsConfig) {
      String region = containerServiceParams.getRegion();
      String cluster = containerServiceParams.getClusterName();
      return "ECS Cluster: " + cluster + ", " + getAwsRegionCriteria(region);
    } else if (value instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) value;
      if (kubernetesClusterConfig.isUseKubernetesDelegate()) {
        return "delegate-name: " + kubernetesClusterConfig.getDelegateName();
      }
      return kubernetesClusterConfig.getMasterUrl();
    } else if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    } else if (value instanceof GcpConfig) {
      return "GCP:" + containerServiceParams.getClusterName();
    } else if (value instanceof AzureConfig) {
      String subscriptionId = containerServiceParams.getSubscriptionId();
      String resourceGroup = containerServiceParams.getResourceGroup();
      return "Azure:" + subscriptionId + resourceGroup + containerServiceParams.getClusterName();
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Unknown kubernetes cloud provider setting value: " + value.getType());
    }
  }

  private String getKubernetesMasterUrl(ContainerServiceParams containerServiceParams) {
    SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
    SettingValue value = settingAttribute.getValue();
    KubernetesConfig kubernetesConfig;
    if (value instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) value;
    } else {
      String clusterName = containerServiceParams.getClusterName();
      String namespace = containerServiceParams.getNamespace();
      String subscriptionId = containerServiceParams.getSubscriptionId();
      String resourceGroup = containerServiceParams.getResourceGroup();
      List<EncryptedDataDetail> edd = containerServiceParams.getEncryptionDetails();
      if (value instanceof GcpConfig) {
        kubernetesConfig = gkeClusterService.getCluster(settingAttribute, edd, clusterName, namespace);
      } else if (value instanceof AzureConfig) {
        AzureConfig azureConfig = (AzureConfig) value;
        kubernetesConfig = azureHelperService.getKubernetesClusterConfig(
            azureConfig, edd, subscriptionId, resourceGroup, clusterName, namespace);
      } else if (value instanceof KubernetesClusterConfig) {
        return ((KubernetesClusterConfig) value).getMasterUrl();
      } else {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Unknown kubernetes cloud provider setting value: " + value.getType());
      }
    }
    return kubernetesConfig.getMasterUrl();
  }

  private String getAwsRegionCriteria(String region) {
    return region == null ? ALWAYS_TRUE_CRITERIA : "AWS Region: " + region;
  }
}

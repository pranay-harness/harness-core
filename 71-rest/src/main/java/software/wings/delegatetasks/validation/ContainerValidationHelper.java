package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.network.Http.connectableHttpUrl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.util.List;

@Singleton
@Slf4j
public class ContainerValidationHelper {
  private static final String ALWAYS_TRUE_CRITERIA = "ALWAYS_TRUE_CRITERIA";

  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient AzureHelperService azureHelperService;
  @Inject @Transient private transient EncryptionService encryptionService;

  boolean validateContainerServiceParams(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();

    // see if we can decrypt from this delegate
    if (value instanceof EncryptableSetting && !value.isDecrypted()
        && isNotEmpty(containerServiceParams.getEncryptionDetails())) {
      try {
        encryptionService.decrypt((EncryptableSetting) value, containerServiceParams.getEncryptionDetails());
      } catch (Exception e) {
        logger.info("failed to decrypt " + value, e);
        return false;
      }
    }

    boolean validated;
    if (value instanceof AwsConfig) {
      validated = true;
    } else if (value instanceof KubernetesClusterConfig
        && ((KubernetesClusterConfig) value).isUseKubernetesDelegate()) {
      validated = ((KubernetesClusterConfig) value).getDelegateName().equals(System.getenv().get("DELEGATE_NAME"));
    } else {
      String url;
      url = "None".equals(containerServiceParams.getClusterName()) ? "https://container.googleapis.com/"
                                                                   : getKubernetesMasterUrl(containerServiceParams);
      validated = connectableHttpUrl(url);
    }

    return validated;
  }

  public String getCriteria(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof AwsConfig) {
      return ALWAYS_TRUE_CRITERIA;
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

  public String getK8sMasterUrl(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof EncryptableSetting && !value.isDecrypted()
        && isNotEmpty(containerServiceParams.getEncryptionDetails())) {
      try {
        encryptionService.decrypt((EncryptableSetting) value, containerServiceParams.getEncryptionDetails());
      } catch (Exception e) {
        logger.info("failed to decrypt " + value, e);
        return null;
      }
    }
    return getKubernetesMasterUrl(containerServiceParams);
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
}

package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.network.Http.connectableHttpUrl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.util.List;

@Singleton
public class K8sValidationHelper {
  private static final Logger logger = LoggerFactory.getLogger(K8sValidationHelper.class);
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient AzureHelperService azureHelperService;
  @Inject @Transient private transient EncryptionService encryptionService;

  boolean validateContainerServiceParams(K8sClusterConfig k8sClusterConfig) {
    SettingValue value = k8sClusterConfig.getCloudProvider();

    // see if we can decrypt from this delegate
    if (isNotEmpty(k8sClusterConfig.getCloudProviderEncryptionDetails())) {
      try {
        encryptionService.decrypt((EncryptableSetting) value, k8sClusterConfig.getCloudProviderEncryptionDetails());
      } catch (Exception e) {
        logger.info("failed to decrypt " + value, e);
        return false;
      }
    }

    boolean validated;
    if (value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate()) {
      validated = ((KubernetesClusterConfig) value).getDelegateName().equals(System.getenv().get("DELEGATE_NAME"));
    } else {
      String url;
      url = getKubernetesMasterUrl(k8sClusterConfig);
      validated = connectableHttpUrl(url);
    }

    return validated;
  }

  public String getCriteria(K8sClusterConfig k8sClusterConfig) {
    SettingValue value = k8sClusterConfig.getCloudProvider();
    if (value instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) value;
      if (kubernetesClusterConfig.isUseKubernetesDelegate()) {
        return "delegate-name: " + kubernetesClusterConfig.getDelegateName();
      }
      return kubernetesClusterConfig.getMasterUrl();
    } else if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    } else if (value instanceof GcpConfig) {
      return "GCP:" + k8sClusterConfig.getGcpKubernetesCluster().getClusterName();
    } else if (value instanceof AzureConfig) {
      String subscriptionId = k8sClusterConfig.getAzureKubernetesCluster().getSubscriptionId();
      String resourceGroup = k8sClusterConfig.getAzureKubernetesCluster().getResourceGroup();
      return "Azure:" + subscriptionId + resourceGroup + k8sClusterConfig.getAzureKubernetesCluster().getName();
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Unknown kubernetes cloud provider setting value: " + value.getType());
    }
  }

  private String getKubernetesMasterUrl(K8sClusterConfig k8sClusterConfig) {
    SettingValue value = k8sClusterConfig.getCloudProvider();
    KubernetesConfig kubernetesConfig;
    if (value instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) value;
    } else {
      String namespace = k8sClusterConfig.getNamespace();
      List<EncryptedDataDetail> edd = k8sClusterConfig.getCloudProviderEncryptionDetails();
      if (value instanceof GcpConfig) {
        kubernetesConfig = gkeClusterService.getCluster(
            (GcpConfig) value, edd, k8sClusterConfig.getGcpKubernetesCluster().getClusterName(), namespace);
      } else if (value instanceof AzureConfig) {
        AzureConfig azureConfig = (AzureConfig) value;
        kubernetesConfig = azureHelperService.getKubernetesClusterConfig(azureConfig, edd,
            k8sClusterConfig.getAzureKubernetesCluster().getSubscriptionId(),
            k8sClusterConfig.getAzureKubernetesCluster().getResourceGroup(),
            k8sClusterConfig.getAzureKubernetesCluster().getName(), namespace);
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

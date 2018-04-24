package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.settings.SettingValue;

import java.util.List;

@Singleton
public class ContainerValidationHelper {
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient AzureHelperService azureHelperService;

  public List<DelegateConnectionResult> validateContainerServiceParams(ContainerServiceParams containerServiceParams) {
    String criteria = getCriteria(containerServiceParams);

    DelegateConnectionResultBuilder result = DelegateConnectionResult.builder().criteria(criteria);
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();

    if (value instanceof AwsConfig) {
      result.validated(AwsHelperService.isInAwsRegion(substringAfter(criteria, ":")));
    } else {
      result.validated(connectableHttpUrl(getKubernetesMasterUrl(containerServiceParams)));
    }

    return singletonList(result.build());
  }

  public String getCriteria(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof AwsConfig) {
      return "AWS:" + containerServiceParams.getRegion();
    } else {
      return getKubernetesMasterUrl(containerServiceParams);
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
}

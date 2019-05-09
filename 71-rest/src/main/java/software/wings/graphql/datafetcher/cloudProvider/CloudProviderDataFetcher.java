package software.wings.graphql.datafetcher.cloudProvider;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLCloudProviderQueryParameters;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.settings.SettingValue;

@Slf4j
public class CloudProviderDataFetcher extends AbstractDataFetcher<QLCloudProvider, QLCloudProviderQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLCloudProvider fetch(QLCloudProviderQueryParameters qlQuery) {
    SettingAttribute settingAttribute = persistence.get(SettingAttribute.class, qlQuery.getCloudProviderId());
    if (settingAttribute == null) {
      throw new InvalidRequestException("Cloud Provider does not exist", WingsException.USER);
    }

    SettingValue settingValue = settingAttribute.getValue();
    if (settingValue instanceof PhysicalDataCenterConfig) {
      return CloudProviderController.preparePhysicalDataCenterConfig(settingAttribute);
    }

    if (settingValue instanceof AwsConfig) {
      return CloudProviderController.prepareAwsConfig(settingAttribute);
    }

    if (settingValue instanceof GcpConfig) {
      return CloudProviderController.prepareGcpConfig(settingAttribute);
    }

    if (settingValue instanceof KubernetesClusterConfig) {
      return CloudProviderController.prepareKubernetesConfig(settingAttribute);
    }

    if (settingValue instanceof AzureConfig) {
      return CloudProviderController.prepareAzureConfig(settingAttribute);
    }

    if (settingValue instanceof PcfConfig) {
      return CloudProviderController.preparePcfConfig(settingAttribute);
    }

    throw new WingsException("Invalid Cloud Provider Type");
  }
}

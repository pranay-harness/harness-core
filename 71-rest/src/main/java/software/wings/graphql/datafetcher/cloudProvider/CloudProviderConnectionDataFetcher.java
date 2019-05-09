package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLCloudProvidersQueryParameters;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection.QLCloudProviderConnectionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import java.util.List;

@Slf4j
public class CloudProviderConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLCloudProviderConnection, QLCloudProvidersQueryParameters> {
  @Inject private SettingsService settingsService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLCloudProviderConnection fetchConnection(QLCloudProvidersQueryParameters parameters) {
    final List<SettingAttribute> settingAttributes =
        persistence.createAuthorizedQuery(SettingAttribute.class)
            .filter(SettingAttributeKeys.accountId, parameters.getAccountId())
            .filter(SettingAttributeKeys.category, SettingCategory.CLOUD_PROVIDER)
            .asList();

    final List<SettingAttribute> filteredSettingAttributes =
        settingsService.getFilteredSettingAttributes(settingAttributes, null, null);

    QLCloudProviderConnectionBuilder qlCloudProviderConnectionBuilder = QLCloudProviderConnection.builder();

    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder().hasMore(false).offset(0).limit(0).total(0);

    if (isNotEmpty(filteredSettingAttributes)) {
      pageInfoBuilder.total(filteredSettingAttributes.size()).limit(filteredSettingAttributes.size());

      for (SettingAttribute settingAttribute : filteredSettingAttributes) {
        SettingValue settingValue = settingAttribute.getValue();
        if (settingValue instanceof PhysicalDataCenterConfig) {
          qlCloudProviderConnectionBuilder.node(
              CloudProviderController.preparePhysicalDataCenterConfig(settingAttribute));
        } else if (settingValue instanceof AwsConfig) {
          qlCloudProviderConnectionBuilder.node(CloudProviderController.prepareAwsConfig(settingAttribute));
        } else if (settingValue instanceof GcpConfig) {
          qlCloudProviderConnectionBuilder.node(CloudProviderController.prepareGcpConfig(settingAttribute));
        } else if (settingValue instanceof AzureConfig) {
          qlCloudProviderConnectionBuilder.node(CloudProviderController.prepareAzureConfig(settingAttribute));
        } else if (settingValue instanceof KubernetesClusterConfig) {
          qlCloudProviderConnectionBuilder.node(CloudProviderController.prepareKubernetesConfig(settingAttribute));
        } else if (settingValue instanceof PcfConfig) {
          qlCloudProviderConnectionBuilder.node(CloudProviderController.preparePcfConfig(settingAttribute));
        }
      }
    }

    return qlCloudProviderConnectionBuilder.build();
  }
}

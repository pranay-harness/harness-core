package software.wings.graphql.datafetcher.cloudProvider;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.cloudProvider.QLAwsCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLAwsCloudProvider.QLAwsCloudProviderBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLAzureCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLAzureCloudProvider.QLAzureCloudProviderBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLGcpCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLGcpCloudProvider.QLGcpCloudProviderBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider.QLKubernetesClusterCloudProviderBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLPcfCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPcfCloudProvider.QLPcfCloudProviderBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider.QLPhysicalDataCenterCloudProviderBuilder;

public class CloudProviderController {
  public static void populateCloudProvider(SettingAttribute settingAttribute, QLCloudProviderBuilder builder) {
    builder.id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .createdAt(GraphQLDateTimeScalar.convert(settingAttribute.getCreatedAt()))
        .createdBy(UserController.populateUser(settingAttribute.getCreatedBy()));
  }

  public static QLPhysicalDataCenterCloudProvider preparePhysicalDataCenterConfig(SettingAttribute settingAttribute) {
    QLPhysicalDataCenterCloudProviderBuilder qlPhysicalDataCenterConfigBuilder =
        QLPhysicalDataCenterCloudProvider.builder();
    CloudProviderController.populateCloudProvider(settingAttribute, qlPhysicalDataCenterConfigBuilder);

    return qlPhysicalDataCenterConfigBuilder.build();
  }

  public static QLAwsCloudProvider prepareAwsConfig(SettingAttribute settingAttribute) {
    QLAwsCloudProviderBuilder qlAwsCloudProviderBuilder = QLAwsCloudProvider.builder();
    CloudProviderController.populateCloudProvider(settingAttribute, qlAwsCloudProviderBuilder);

    return qlAwsCloudProviderBuilder.build();
  }

  public static QLGcpCloudProvider prepareGcpConfig(SettingAttribute settingAttribute) {
    QLGcpCloudProviderBuilder qlGcpConfigBuilder = QLGcpCloudProvider.builder();
    CloudProviderController.populateCloudProvider(settingAttribute, qlGcpConfigBuilder);

    return qlGcpConfigBuilder.build();
  }

  public static QLAzureCloudProvider prepareAzureConfig(SettingAttribute settingAttribute) {
    QLAzureCloudProviderBuilder qlAzureConfigBuilder = QLAzureCloudProvider.builder();
    CloudProviderController.populateCloudProvider(settingAttribute, qlAzureConfigBuilder);

    return qlAzureConfigBuilder.build();
  }

  public static QLKubernetesClusterCloudProvider prepareKubernetesConfig(SettingAttribute settingAttribute) {
    QLKubernetesClusterCloudProviderBuilder qlKubernetesClusterConfigBuilder =
        QLKubernetesClusterCloudProvider.builder();
    CloudProviderController.populateCloudProvider(settingAttribute, qlKubernetesClusterConfigBuilder);

    return qlKubernetesClusterConfigBuilder.build();
  }

  public static QLPcfCloudProvider preparePcfConfig(SettingAttribute settingAttribute) {
    QLPcfCloudProviderBuilder qlPcfConfigBuilder = QLPcfCloudProvider.builder();
    CloudProviderController.populateCloudProvider(settingAttribute, qlPcfConfigBuilder);

    return qlPcfConfigBuilder.build();
  }
}

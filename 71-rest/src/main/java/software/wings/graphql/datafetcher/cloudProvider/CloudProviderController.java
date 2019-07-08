package software.wings.graphql.datafetcher.cloudProvider;

import io.harness.exception.WingsException;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.cloudProvider.QLAwsCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLAzureCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderBuilder;
import software.wings.graphql.schema.type.cloudProvider.QLGcpCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPcfCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;

public class CloudProviderController {
  public static QLCloudProviderBuilder populateCloudProvider(
      SettingAttribute settingAttribute, QLCloudProviderBuilder builder) {
    return builder.id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .createdAt(GraphQLDateTimeScalar.convert(settingAttribute.getCreatedAt()))
        .createdBy(UserController.populateUser(settingAttribute.getCreatedBy()));
  }

  public static QLCloudProviderBuilder getCloudProviderBuilder(SettingAttribute settingAttribute) {
    QLCloudProviderBuilder cloudProviderBuilder;
    switch (settingAttribute.getValue().getSettingType()) {
      case AZURE:
        cloudProviderBuilder = QLAzureCloudProvider.builder();
        break;
      case PHYSICAL_DATA_CENTER:
        cloudProviderBuilder = QLPhysicalDataCenterCloudProvider.builder();
        break;
      case AWS:
        cloudProviderBuilder = QLAwsCloudProvider.builder();
        break;
      case GCP:
        cloudProviderBuilder = QLGcpCloudProvider.builder();
        break;
      case KUBERNETES:
      case KUBERNETES_CLUSTER:
        cloudProviderBuilder = QLKubernetesClusterCloudProvider.builder();
        break;
      case PCF:
        cloudProviderBuilder = QLPcfCloudProvider.builder();
        break;
      default:
        throw new WingsException("Unknown Cloud ProviderType " + settingAttribute.getValue().getSettingType());
    }

    return cloudProviderBuilder;
  }
}

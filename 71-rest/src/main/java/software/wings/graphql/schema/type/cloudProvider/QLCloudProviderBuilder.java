package software.wings.graphql.schema.type.cloudProvider;

import io.harness.ccm.health.CEHealthStatus;
import software.wings.graphql.schema.type.QLUser;

public interface QLCloudProviderBuilder {
  QLCloudProviderBuilder id(String id);
  QLCloudProviderBuilder name(String name);
  QLCloudProviderBuilder createdAt(Long createdAt);
  QLCloudProviderBuilder createdBy(QLUser createdBy);
  QLCloudProviderBuilder type(String type);
  QLCloudProviderBuilder isContinuousEfficiencyEnabled(boolean isContinuousEfficiencyEnabled);
  QLCloudProviderBuilder ceHealthStatus(CEHealthStatus ceHealthStatus);
  QLCloudProvider build();
}

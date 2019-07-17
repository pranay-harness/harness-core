package software.wings.graphql.schema.type.cloudProvider;

import software.wings.graphql.schema.type.QLUser;

public interface QLCloudProviderBuilder {
  QLCloudProviderBuilder id(String id);
  QLCloudProviderBuilder name(String name);
  QLCloudProviderBuilder createdAt(Long createdAt);
  QLCloudProviderBuilder createdBy(QLUser createdBy);
  QLCloudProvider build();
}

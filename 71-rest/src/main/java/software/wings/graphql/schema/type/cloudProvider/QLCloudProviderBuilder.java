package software.wings.graphql.schema.type.cloudProvider;

import software.wings.graphql.schema.type.QLUser;

import java.time.OffsetDateTime;

public interface QLCloudProviderBuilder {
  QLCloudProviderBuilder id(String id);
  QLCloudProviderBuilder name(String name);
  QLCloudProviderBuilder createdAt(OffsetDateTime createdAt);
  QLCloudProviderBuilder createdBy(QLUser createdBy);
}

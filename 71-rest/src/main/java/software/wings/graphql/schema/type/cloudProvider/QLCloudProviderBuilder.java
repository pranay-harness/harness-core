package software.wings.graphql.schema.type.cloudProvider;

import software.wings.graphql.schema.type.QLUser;

import java.time.ZonedDateTime;

public interface QLCloudProviderBuilder {
  QLCloudProviderBuilder id(String id);
  QLCloudProviderBuilder name(String name);
  QLCloudProviderBuilder createdAt(ZonedDateTime createdAt);
  QLCloudProviderBuilder createdBy(QLUser createdBy);
}

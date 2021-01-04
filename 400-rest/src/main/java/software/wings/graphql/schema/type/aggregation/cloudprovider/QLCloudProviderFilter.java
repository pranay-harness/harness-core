package software.wings.graphql.schema.type.aggregation.cloudprovider;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCloudProviderFilter implements EntityFilter {
  QLIdFilter cloudProvider;
  QLCloudProviderTypeFilter cloudProviderType;
  QLTimeFilter createdAt;
}

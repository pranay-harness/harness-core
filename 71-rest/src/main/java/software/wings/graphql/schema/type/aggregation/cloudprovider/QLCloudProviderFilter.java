package software.wings.graphql.schema.type.aggregation.cloudprovider;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

@Value
@Builder
public class QLCloudProviderFilter implements EntityFilter {
  QLIdFilter cloudProvider;
  QLStringFilter cloudProviderType;
  QLTimeFilter createdAt;
}
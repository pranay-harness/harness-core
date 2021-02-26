package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLCloudProviderFilter implements EntityFilter {
  QLIdFilter cloudProvider;
  QLCloudProviderTypeFilter cloudProviderType;
  QLTimeFilter createdAt;
}

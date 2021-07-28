package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public class QLCloudProviderFilter implements EntityFilter {
  QLIdFilter cloudProvider;
  QLCloudProviderTypeFilter cloudProviderType;
  QLCEEnabledFilter isCEEnabled;
  QLTimeFilter createdAt;
}

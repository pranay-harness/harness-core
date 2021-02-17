package software.wings.graphql.datafetcher.secretManager;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLSecretManagerTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLSecretManagerType[] values;
}

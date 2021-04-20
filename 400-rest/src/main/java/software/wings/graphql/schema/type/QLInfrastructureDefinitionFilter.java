package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLInfrastructureDefinitionFilter {
  QLIdFilter environment;
  QLIdFilter infrastructureDefinition;
  QLStringFilter deploymentType;
  QLInfrastructureDefinitionTagFilter tag;
}

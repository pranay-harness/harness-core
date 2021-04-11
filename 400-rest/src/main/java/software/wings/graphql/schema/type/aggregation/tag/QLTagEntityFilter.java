package software.wings.graphql.schema.type.aggregation.tag;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLTagEntityFilter implements EntityFilter {
  QLIdFilter tagId;
  QLIdFilter tagName;
}

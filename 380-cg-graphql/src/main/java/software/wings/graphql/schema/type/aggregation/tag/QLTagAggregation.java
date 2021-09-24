package software.wings.graphql.schema.type.aggregation.tag;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.aggregation.QLEntityType;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 08/26/2019
 */
@Value
@Builder
@OwnedBy(HarnessTeam.CDC)
public class QLTagAggregation {
  private QLEntityType entityType;
  private String tagName;
}

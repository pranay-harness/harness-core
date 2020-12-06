package software.wings.graphql.schema.type.aggregation.service;

import software.wings.graphql.schema.type.aggregation.TagAggregation;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLServiceTagAggregation implements TagAggregation {
  private QLServiceTagType entityType;
  private String tagName;
}

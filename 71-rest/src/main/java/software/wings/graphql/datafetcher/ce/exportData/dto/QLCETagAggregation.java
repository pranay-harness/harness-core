package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.schema.type.aggregation.TagAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCETagAggregation implements TagAggregation {
  private QLCETagType entityType;
  private String tagName;
}

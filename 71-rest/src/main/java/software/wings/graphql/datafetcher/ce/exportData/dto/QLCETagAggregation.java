package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

@Value
@Builder
public class QLCETagAggregation implements TagAggregation {
  private QLCETagType entityType;
  private String tagName;
}

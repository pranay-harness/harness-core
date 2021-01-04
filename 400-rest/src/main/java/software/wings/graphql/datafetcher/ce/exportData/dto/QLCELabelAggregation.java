package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.schema.type.aggregation.LabelAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCELabelAggregation implements LabelAggregation {
  String name;
}

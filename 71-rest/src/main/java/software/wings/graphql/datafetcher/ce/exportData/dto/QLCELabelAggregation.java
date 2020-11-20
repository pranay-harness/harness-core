package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.LabelAggregation;

@Value
@Builder
public class QLCELabelAggregation implements LabelAggregation {
  String name;
}

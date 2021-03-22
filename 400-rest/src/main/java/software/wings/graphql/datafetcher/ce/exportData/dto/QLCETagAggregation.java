package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.TagAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCETagAggregation implements TagAggregation {
  private QLCETagType entityType;
  private String tagName;
}

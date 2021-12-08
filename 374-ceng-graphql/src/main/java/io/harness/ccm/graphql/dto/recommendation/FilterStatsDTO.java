package io.harness.ccm.graphql.dto.recommendation;

import io.leangen.graphql.annotations.types.GraphQLType;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@GraphQLType(name = "FilterStats")
public class FilterStatsDTO {
  String key;
  List<String> values;
}
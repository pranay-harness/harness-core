package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@GraphQLType(name = "ViewFilterWrapper")
public class QLCEViewFilterWrapper {
  QLCEViewFilter idFilter;
  QLCEViewTimeFilter timeFilter;
  QLCEViewMetadataFilter viewMetadataFilter;
  QLCEViewRule ruleFilter;
}

package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PUBLIC)
public class QLCEViewTimeFilter {
  @GraphQLNonNull QLCEViewFieldInput field;
  @GraphQLNonNull QLCEViewTimeFilterOperator operator;
  @GraphQLNonNull Number value;
}

package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@GraphQLType(name = "ViewFilter")
public class QLCEViewFilter {
  @GraphQLNonNull QLCEViewFieldInput field;
  @GraphQLNonNull QLCEViewFilterOperator operator;
  @GraphQLNonNull String[] values;
}

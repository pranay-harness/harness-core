package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PUBLIC)
@GraphQLType(name = "ViewTimeFilter")
public class QLCEViewTimeFilter {
  @GraphQLNonNull QLCEViewFieldInput field;
  @GraphQLNonNull QLCEViewTimeFilterOperator operator;
  @GraphQLNonNull Number value;
}

package io.harness.ccm.views.graphql;

import io.leangen.graphql.annotations.types.GraphQLType;

@GraphQLType(name = "ViewFilterOperator")
public enum QLCEViewFilterOperator {
  NOT_IN,
  IN,
  EQUALS,
  NOT_NULL,
  NULL,
  LIKE
}

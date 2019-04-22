package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;

public interface QLPageQueryParameters {
  int getLimit();
  int getOffset();
  DataFetchingFieldSelectionSet getSelectionSet();

  default boolean isHasMoreRequested() {
    return getSelectionSet().contains("pageInfo/hasMore");
  }

  default boolean isTotalRequested() {
    return getSelectionSet().contains("pageInfo/total");
  }
}

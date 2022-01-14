package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLPageQueryParameters {
  int getLimit();
  int getOffset();
  DataFetchingFieldSelectionSet getSelectionSet();

  default DataFetchingEnvironment getDataFetchingEnvironment() {
    return null;
  };

  default boolean isHasMoreRequested() {
    return getSelectionSet().contains("pageInfo/hasMore");
  }

  default boolean isTotalRequested() {
    return getSelectionSet().contains("pageInfo/total");
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

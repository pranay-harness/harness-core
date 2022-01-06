/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

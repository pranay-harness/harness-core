/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLOutcomesQueryParameters {
  private String executionId;

  private DataFetchingFieldSelectionSet selectionSet;

  public boolean isServiceRequested() {
    // TODO: it is not trivial how to find out if a field in inline fragment is selected
    return true;
  }

  public boolean isEnvironmentRequested() {
    // TODO: it is not trivial how to find out if a field in inline fragment is selected
    return true;
  }
}

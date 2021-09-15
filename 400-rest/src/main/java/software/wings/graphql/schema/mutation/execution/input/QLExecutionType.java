/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLExecutionType implements QLEnum {
  WORKFLOW,
  PIPELINE;

  @Override
  public String getStringValue() {
    return this.name();
  }
}

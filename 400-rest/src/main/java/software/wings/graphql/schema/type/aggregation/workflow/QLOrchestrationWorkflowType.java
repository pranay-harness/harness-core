/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type.aggregation.workflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public enum QLOrchestrationWorkflowType implements QLEnum {
  BUILD,
  BASIC,
  CANARY,
  MULTI_SERVICE,
  BLUE_GREEN,
  ROLLING,
  CUSTOM;

  @Override
  public String getStringValue() {
    return this.name();
  }
}

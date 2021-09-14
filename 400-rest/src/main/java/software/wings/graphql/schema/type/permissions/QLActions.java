/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.permissions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLActions implements QLEnum {
  CREATE,
  READ,
  UPDATE,
  DELETE,
  @Deprecated EXECUTE,
  EXECUTE_WORKFLOW,
  EXECUTE_PIPELINE,
  ROLLBACK_WORKFLOW;
  @Override
  public String getStringValue() {
    return this.name();
  }
}

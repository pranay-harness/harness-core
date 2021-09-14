/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLPermissionType implements QLEnum {
  ALL,
  ENV,
  SERVICE,
  WORKFLOW,
  PIPELINE,
  DEPLOYMENT,
  PROVISIONER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}

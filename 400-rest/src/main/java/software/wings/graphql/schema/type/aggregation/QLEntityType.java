/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLEntityType implements QLEnum {
  APPLICATION,
  SERVICE,
  ENVIRONMENT,
  WORKFLOW,
  PIPELINE,
  INSTANCE,
  DEPLOYMENT,
  CLOUD_PROVIDER,
  CONNECTOR,
  TRIGGER,
  ARTIFACT,
  COLLABORATION_PROVIDER,
  PROVISIONER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}

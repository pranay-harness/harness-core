/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.instance;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.artifact.QLArtifact;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLInstance {
  String getId();
  QLInstanceType getType();
  String getEnvironmentId();
  String getApplicationId();
  String getServiceId();
  QLArtifact getArtifact();
}

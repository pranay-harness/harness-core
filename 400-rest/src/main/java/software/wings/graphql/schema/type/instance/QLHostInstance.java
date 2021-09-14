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

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public abstract class QLHostInstance implements QLInstance, QLPhysicalHost {
  private String hostId;
  private String hostName;
  private String hostPublicDns;

  private String id;
  private QLInstanceType type;
  private String environmentId;
  private String applicationId;
  private String serviceId;
  private QLArtifact artifact;
}

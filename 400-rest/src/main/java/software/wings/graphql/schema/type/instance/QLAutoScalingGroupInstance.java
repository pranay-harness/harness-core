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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAutoScalingGroupInstance extends QLAbstractEc2Instance {
  private String autoScalingGroupName;

  @Builder
  public QLAutoScalingGroupInstance(String hostId, String hostName, String hostPublicDns, String id,
      QLInstanceType type, String environmentId, String applicationId, String serviceId, QLArtifact artifact,
      String autoScalingGroupName) {
    super(hostId, hostName, hostPublicDns, id, type, environmentId, applicationId, serviceId, artifact);
    this.autoScalingGroupName = autoScalingGroupName;
  }
}

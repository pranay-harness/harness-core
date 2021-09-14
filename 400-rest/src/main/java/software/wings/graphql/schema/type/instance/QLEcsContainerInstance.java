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
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEcsContainerInstance extends QLContainerInstance {
  private String taskArn;
  private String taskDefinitionArn;
  private String serviceName;
  private long version;
  private Long startedAt;
  private String startedBy;

  @Builder
  public QLEcsContainerInstance(String id, QLInstanceType type, String environmentId, String applicationId,
      String serviceId, QLArtifact artifact, String clusterName, String identifier, String taskArn,
      String taskDefinitionArn, String serviceName, long version, Long startedAt, String startedBy) {
    super(id, type, environmentId, applicationId, serviceId, artifact, clusterName, identifier);
    this.taskArn = taskArn;
    this.taskDefinitionArn = taskDefinitionArn;
    this.serviceName = serviceName;
    this.version = version;
    this.startedAt = startedAt;
    this.startedBy = startedBy;
  }
}

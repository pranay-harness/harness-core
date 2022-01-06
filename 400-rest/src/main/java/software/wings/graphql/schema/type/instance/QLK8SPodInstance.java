/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.instance;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLK8SPodInstance extends QLContainerInstance {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<QLK8sContainer> containers;

  @Builder
  public QLK8SPodInstance(String id, QLInstanceType type, String environmentId, String applicationId, String serviceId,
      QLArtifact artifact, String clusterName, String identifier, String releaseName, String podName, String ip,
      String namespace, List<QLK8sContainer> containers) {
    super(id, type, environmentId, applicationId, serviceId, artifact, clusterName, identifier);
    this.releaseName = releaseName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.containers = containers;
  }
}

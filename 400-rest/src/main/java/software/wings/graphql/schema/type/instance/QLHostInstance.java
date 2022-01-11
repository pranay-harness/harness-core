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

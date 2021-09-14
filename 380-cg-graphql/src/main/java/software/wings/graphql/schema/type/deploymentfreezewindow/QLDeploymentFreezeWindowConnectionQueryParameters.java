/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.deploymentfreezewindow;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@Value
@OwnedBy(HarnessTeam.CDC)
public class QLDeploymentFreezeWindowConnectionQueryParameters {
  private Boolean listEnabled;
  private Integer limit;
  private Integer offset;
}

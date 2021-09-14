/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instance.sync.request;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;

import java.util.Collection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
@TargetModule(HarnessModule._957_CG_BEANS)
public class ContainerFilter {
  private Collection<ContainerDeploymentInfo> containerDeploymentInfoCollection;
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 4/3/18.
 */
@Data
@Builder
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class HelmDeployContextElement implements ContextElement {
  private String releaseName;
  private Integer previousReleaseRevision;
  private Integer newReleaseRevision;
  private String commandFlags;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.HELM_DEPLOY;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return null;
  }
}

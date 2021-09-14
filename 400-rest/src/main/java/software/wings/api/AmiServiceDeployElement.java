/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;

import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.beans.InstanceUnitType;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 12/22/17.
 */
@Data
@Builder
@TargetModule(_957_CG_BEANS)
public class AmiServiceDeployElement implements ContextElement {
  private int instanceCount;
  private InstanceUnitType instanceUnitType;
  private List<ContainerServiceData> newInstanceData = Lists.newArrayList();
  private List<ContainerServiceData> oldInstanceData = Lists.newArrayList();

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AMI_SERVICE_DEPLOY;
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

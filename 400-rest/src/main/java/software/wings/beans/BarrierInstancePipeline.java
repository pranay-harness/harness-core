/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "BarrierInstancePipelineKeys")
public class BarrierInstancePipeline {
  private String executionId;
  private int parallelIndex;
  List<BarrierInstanceWorkflow> workflows;
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.stepstatus;

import static io.harness.delegate.task.stepstatus.StepOutput.Type.MAP;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@JsonTypeName("MAP")
public class StepMapOutput implements StepOutput {
  @Singular("output") Map<String, String> map;
  @Override
  public StepOutput.Type getType() {
    return MAP;
  }
}

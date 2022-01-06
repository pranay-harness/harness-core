/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

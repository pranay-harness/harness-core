/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProcessExecutorCapabilityGenerator {
  public static ProcessExecutorCapability buildProcessExecutorCapability(
      String category, List<String> processExecutorArguments) {
    return ProcessExecutorCapability.builder()
        .category(category)
        .processExecutorArguments(processExecutorArguments)
        .build();
  }
}

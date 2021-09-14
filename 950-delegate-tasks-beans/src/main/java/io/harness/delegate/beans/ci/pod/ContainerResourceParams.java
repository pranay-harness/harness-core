/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerResourceParams {
  private Integer resourceRequestMemoryMiB;
  private Integer resourceLimitMemoryMiB;
  private Integer resourceRequestMilliCpu;
  private Integer resourceLimitMilliCpu;
}

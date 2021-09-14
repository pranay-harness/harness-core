/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.constraint;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RunnableConsumers {
  private int usedPermits;
  private List<ConsumerId> consumerIds;
}

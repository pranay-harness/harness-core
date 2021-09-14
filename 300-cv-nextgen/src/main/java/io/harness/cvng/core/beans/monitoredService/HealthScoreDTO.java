/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.monitoredService;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HealthScoreDTO {
  RiskData currentHealthScore;
  // TODO: dependency health score to be added in here.
}

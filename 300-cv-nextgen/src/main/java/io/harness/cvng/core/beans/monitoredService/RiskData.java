/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.core.beans.params.TimeRangeParams;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskData {
  Integer healthScore;
  Risk riskStatus;
  TimeRangeParams timeRangeParams;
}

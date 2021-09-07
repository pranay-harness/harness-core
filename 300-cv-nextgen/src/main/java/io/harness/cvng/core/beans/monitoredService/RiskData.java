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

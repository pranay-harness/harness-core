package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.analysis.beans.Risk;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskData {
  Integer healthScore;
  Risk riskStatus;
}

package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.analysis.beans.Risk;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskData {
  Integer healthScore;
  Risk riskStatus;

  public int getRiskValue() {
    int riskValue = -2;
    if (healthScore != null) {
      riskValue = 100 - healthScore.intValue();
    }
    return riskValue;
  }
}

package software.wings.metrics;

/**
 * Created by mike@ on 4/11/17.
 */
public enum RiskLevel {
  HIGH(2),
  MEDIUM(1),
  LOW(0),
  NA(-1);

  private int risk;

  RiskLevel(int risk) {
    this.risk = risk;
  }

  public static RiskLevel getRiskLevel(int risk) {
    RiskLevel riskLevel;
    switch (risk) {
      case -1:
        riskLevel = RiskLevel.NA;
        break;
      case 0:
        riskLevel = RiskLevel.LOW;
        break;
      case 1:
        riskLevel = RiskLevel.MEDIUM;
        break;
      case 2:
        riskLevel = RiskLevel.HIGH;
        break;
      default:
        throw new IllegalArgumentException("Unknown risk level " + risk);
    }
    return riskLevel;
  }

  public int getRisk() {
    return risk;
  }
}

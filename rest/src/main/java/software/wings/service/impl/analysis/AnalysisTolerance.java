package software.wings.service.impl.analysis;

/**
 * Created by sriram_parthasarathy on 10/26/17.
 */
public enum AnalysisTolerance {
  LOW("All anomalies", 1),
  MEDIUM("Anomalies with moderate risk or higher", 2),
  HIGH("Anomalies with high risk", 3);

  private final String name;
  int tolerance;

  AnalysisTolerance(String name, int tolerance) {
    this.name = name;
    this.tolerance = tolerance;
  }

  public String getName() {
    return name;
  }

  public int tolerance() {
    return tolerance;
  }
}

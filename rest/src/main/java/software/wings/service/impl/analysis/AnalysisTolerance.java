package software.wings.service.impl.analysis;

/**
 * Created by sriram_parthasarathy on 10/26/17.
 */
public enum AnalysisTolerance {
  LOW("All anomalies"),
  MEDIUM("Anomalies with moderate risk or higher"),
  HIGH("Anomalies with high risk");

  private final String name;

  AnalysisTolerance(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}

package software.wings.service.impl.analysis;

/**
 * Created by rsingh on 7/25/17.
 */
public enum AnalysisComparisonStrategy {
  COMPARE_WITH_PREVIOUS("Compare with previous run"),
  COMPARE_WITH_CURRENT("Compare with current run");

  private final String name;

  AnalysisComparisonStrategy(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}

package io.harness.beans.yaml.extended.reports;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("unit_test_report_type")
public enum UnitTestReportType {
  @JsonProperty("JUnit") JUNIT("JUnit");

  private final String yamlName;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static UnitTestReportType getUnitTestReportType(@JsonProperty("type") String yamlName) {
    for (UnitTestReportType unitTestReportType : UnitTestReportType.values()) {
      if (unitTestReportType.yamlName.equalsIgnoreCase(yamlName)) {
        return unitTestReportType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  UnitTestReportType(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }
}

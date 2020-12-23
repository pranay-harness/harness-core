package io.harness.beans.yaml.extended.reports;

import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.TypeAlias;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UseFromStageInfraYaml.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = JunitTestReport.class, name = "junit") })
public interface UnitTestReport {
  @TypeAlias("unit_test_report_type")
  enum Type {
    JUNIT("junit");

    private final String yamlName;

    Type(String yamlName) {
      this.yamlName = yamlName;
    }

    @JsonValue
    public String getYamlName() {
      return yamlName;
    }
  }
  Type getType();
}

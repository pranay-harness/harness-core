package io.harness.beans.yaml.extended.reports;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@AllArgsConstructor
@JsonTypeName("junit")
@TypeAlias("junit_report")
public class JunitTestReport implements UnitTestReport {
  @Builder.Default private UnitTestReport.Type type = Type.JUNIT;
  Spec spec;

  @Value
  @Builder
  @AllArgsConstructor
  public static class Spec {
    private List<String> paths;
  }
}

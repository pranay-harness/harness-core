package io.harness.delegate.beans.ci.vm.steps;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
@Value
public class JunitTestReport implements UnitTestReport {
  private List<String> paths;

  @Override
  public Type getType() {
    return Type.JUNIT;
  }
}

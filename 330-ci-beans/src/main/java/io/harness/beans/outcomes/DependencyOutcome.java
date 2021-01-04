package io.harness.beans.outcomes;

import io.harness.beans.dependencies.ServiceDependency;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("dependencyOutcome")
@JsonTypeName("dependencyOutcome")
public class DependencyOutcome implements Outcome {
  List<ServiceDependency> serviceDependencyList;

  @Override
  public String getType() {
    return "dependencyOutcome";
  }
}

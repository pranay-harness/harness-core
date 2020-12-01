package io.harness.beans.outcomes;

import io.harness.beans.dependencies.ServiceDependency;
import io.harness.pms.sdk.core.data.Outcome;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("dependencyOutcome")
public class DependencyOutcome implements Outcome {
  List<ServiceDependency> serviceDependencyList;
}

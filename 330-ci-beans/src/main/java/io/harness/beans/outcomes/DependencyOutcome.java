package io.harness.beans.outcomes;

import io.harness.beans.dependencies.ServiceDependency;
import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DependencyOutcome implements Outcome {
  List<ServiceDependency> serviceDependencyList;
}

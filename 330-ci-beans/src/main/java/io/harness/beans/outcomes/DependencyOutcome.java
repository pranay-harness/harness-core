package io.harness.beans.outcomes;

import io.harness.beans.dependencies.ServiceDependency;
import io.harness.data.Outcome;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DependencyOutcome implements Outcome {
  List<ServiceDependency> serviceDependencyList;
}

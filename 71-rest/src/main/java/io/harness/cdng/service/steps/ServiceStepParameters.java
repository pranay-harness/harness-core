package io.harness.cdng.service.steps;

import io.harness.cdng.service.Service;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ServiceStepParameters implements StepParameters {
  Service service;
  @Singular List<String> parallelNodeIds;
}

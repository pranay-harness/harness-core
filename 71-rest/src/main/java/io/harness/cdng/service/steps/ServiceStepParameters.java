package io.harness.cdng.service.steps;

import io.harness.cdng.service.ServiceConfig;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ServiceStepParameters implements StepParameters {
  ServiceConfig service;
  ServiceConfig serviceOverrides;
  @Singular List<String> parallelNodeIds;
}

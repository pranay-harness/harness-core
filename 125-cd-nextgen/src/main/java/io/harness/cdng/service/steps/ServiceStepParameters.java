package io.harness.cdng.service.steps;

import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.state.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ServiceStepParameters implements StepParameters {
  ServiceConfig service;
  ServiceConfig serviceOverrides;
  @Singular List<String> parallelNodeIds;
}

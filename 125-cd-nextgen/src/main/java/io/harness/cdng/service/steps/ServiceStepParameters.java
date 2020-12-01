package io.harness.cdng.service.steps;

import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("serviceStepParameters")
public class ServiceStepParameters implements StepParameters {
  ServiceConfig service;
  ServiceConfig serviceOverrides;
  @Singular List<String> parallelNodeIds;
}

package io.harness.cdng.service.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("serviceStepParameters")
public class ServiceStepParameters implements StepParameters {
  // Overrides from useFromStage are already applied.
  String type;
  String identifier;
  String name;
  ParameterField<String> description;
  Map<String, String> tags;

  ParameterField<String> serviceRefInternal;
  @SkipAutoEvaluation ParameterField<ServiceConfig> serviceConfigInternal;

  public static ServiceStepParameters fromServiceConfig(ServiceConfig serviceConfig) {
    if (serviceConfig == null) {
      return null;
    }

    ServiceYaml service = serviceConfig.getService();
    String type = serviceConfig.getServiceDefinition().getType();
    if (service == null) {
      return ServiceStepParameters.builder()
          .type(type)
          .serviceRefInternal(serviceConfig.getServiceRef())
          .serviceConfigInternal(ParameterField.createValueField(serviceConfig))
          .build();
    }
    return ServiceStepParameters.builder()
        .type(type)
        .identifier(service.getIdentifier())
        .name(service.getName())
        .description(service.getDescription())
        .tags(service.getTags())
        .serviceRefInternal(serviceConfig.getServiceRef())
        .serviceConfigInternal(ParameterField.createValueField(serviceConfig))
        .build();
  }
}

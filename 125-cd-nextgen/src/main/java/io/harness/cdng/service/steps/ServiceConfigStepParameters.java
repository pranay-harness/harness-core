package io.harness.cdng.service.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("serviceConfigStepParameters")
public class ServiceConfigStepParameters implements StepParameters {
  ServiceUseFromStage useFromStage;
  ParameterField<String> serviceRef;

  String childNodeId;

  // Todo(Alexei) Remove this when @RecastIgnore annotation is added
  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toJson(
        ServiceConfigStepParameters.builder().useFromStage(useFromStage).serviceRef(serviceRef).build());
  }
}

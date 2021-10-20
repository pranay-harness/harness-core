package io.harness.cvng.servicelevelobjective.beans;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CV)
@Value
@Builder
public class ServiceLevelObjectiveResponse {
  @NotNull @JsonProperty("serviceLevelObjective") private ServiceLevelObjectiveDTO serviceLevelObjectiveDTO;
  private Long createdAt;
  private Long lastModifiedAt;
}

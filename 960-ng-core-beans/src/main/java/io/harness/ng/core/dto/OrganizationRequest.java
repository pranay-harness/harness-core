package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationRequest {
  @Valid @NotNull @JsonProperty("organization") private OrganizationDTO organization;
}
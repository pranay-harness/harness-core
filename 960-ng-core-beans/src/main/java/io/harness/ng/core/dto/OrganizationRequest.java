package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Schema(description = "This contains details of the Organization.")
public class OrganizationRequest implements YamlDTO {
  @Valid @NotNull @JsonProperty("organization") private OrganizationDTO organization;
}

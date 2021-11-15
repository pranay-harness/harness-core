package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Schema(name = "AccountResources", description = "This is the view of the Account entity defined in Harness")
public class AccountResourcesDTO {
  long connectorsCount;
  long secretsCount;
  long delegatesCount;
  long templatesCount;
}

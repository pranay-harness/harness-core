package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class AccountResourcesDTO {
  long connectorsCount;
  long secretsCount;
  long delegatesCount;
  long templatesCount;
}

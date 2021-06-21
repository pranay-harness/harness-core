package io.harness.gitsync.common.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreatePRDTO {
  int prNumber;
}

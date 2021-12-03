package io.harness.gitsync.common.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "CreatePR", description = "This contains PR Id")
public class CreatePRDTO {
  int prNumber;
}

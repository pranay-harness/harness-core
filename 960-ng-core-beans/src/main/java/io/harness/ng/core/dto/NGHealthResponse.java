package io.harness.ng.core.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NGHealthResponse {
  private boolean healthy;
}

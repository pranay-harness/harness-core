package io.harness.ng.core.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorDetail {
  String reason;
  String message;
  int code;
}

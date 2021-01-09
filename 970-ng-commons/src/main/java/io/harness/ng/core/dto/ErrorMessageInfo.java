package io.harness.ng.core.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorMessageInfo {
  String reason;
  String messageRegex;
  int code;
  String errorCategory;
}

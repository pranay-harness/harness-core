package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class ErrorMessageInfo {
  String reason;
  String messageRegex;
  int code;
  String errorCategory;
  String overriddenMessage;
}

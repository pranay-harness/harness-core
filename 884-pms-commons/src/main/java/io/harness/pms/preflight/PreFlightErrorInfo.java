package io.harness.pms.preflight;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreFlightErrorInfo {
  int count;
  String message;
}

package io.harness.pms.preflight;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreFlightEntityErrorInfo {
  String summary;
  String description;
  List<PreFlightCause> causes;
  List<PreFlightResolution> resolution;
}

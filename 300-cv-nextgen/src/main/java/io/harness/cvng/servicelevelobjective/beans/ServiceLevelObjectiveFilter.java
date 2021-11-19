package io.harness.cvng.servicelevelobjective.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceLevelObjectiveFilter {
  List<String> userJourneys;
  List<String> identifiers;
}

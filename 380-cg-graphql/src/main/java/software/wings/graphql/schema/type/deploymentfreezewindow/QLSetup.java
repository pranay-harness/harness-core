package software.wings.graphql.schema.type.deploymentfreezewindow;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.governance.TimeRangeOccurrence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLSetup {
  Boolean isDurationBased;
  Long duration;
  Long from;
  Long to;
  TimeRangeOccurrence freezeOccurrence;
  Boolean untilForever;
  Long endTime;
}

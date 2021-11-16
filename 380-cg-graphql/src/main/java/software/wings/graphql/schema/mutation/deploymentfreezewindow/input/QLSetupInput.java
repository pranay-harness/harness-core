package software.wings.graphql.schema.mutation.deploymentfreezewindow.input;

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
public class QLSetupInput {
  Boolean isDurationBased;
  Long duration;
  Long from;
  Long to;
  String timeZone;
  TimeRangeOccurrence freezeOccurrence;
  Boolean untilForever;
  Long expiryTime;
}

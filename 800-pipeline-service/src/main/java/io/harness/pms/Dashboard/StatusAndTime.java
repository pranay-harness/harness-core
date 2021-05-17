package io.harness.pms.Dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class StatusAndTime {
  private List<String> time;
  private List<String> status;
}

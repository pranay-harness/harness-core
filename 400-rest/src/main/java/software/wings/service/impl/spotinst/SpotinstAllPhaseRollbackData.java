package software.wings.service.impl.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class SpotinstAllPhaseRollbackData {
  private boolean allPhaseRollbackDone;
}

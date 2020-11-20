package software.wings.service.impl.spotinst;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotinstAllPhaseRollbackData {
  private boolean allPhaseRollbackDone;
}

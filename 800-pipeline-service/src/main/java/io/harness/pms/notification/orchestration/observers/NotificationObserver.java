package io.harness.pms.notification.orchestration.observers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NotificationObserver {
  void onSuccess(Ambiance ambiance);
  void onPause(Ambiance ambiance);
  void onFailure(Ambiance ambiance);
}

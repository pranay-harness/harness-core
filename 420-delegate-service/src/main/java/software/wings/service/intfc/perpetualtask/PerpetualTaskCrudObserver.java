package software.wings.service.intfc.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DEL)
public interface PerpetualTaskCrudObserver {
  void onPerpetualTaskCreated();
  void onRebalanceRequired();
}

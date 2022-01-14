package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class TestNotifyCallback implements OldNotifyCallback {
  @Override
  public void notify(Map<String, ResponseData> response) {
    // NOOP
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // NOOP
  }
}
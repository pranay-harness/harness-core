package io.harness.cvng.cdng.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.waiter.NotifyCallback;
@OwnedBy(HarnessTeam.CV)
public class CVNGAsyncWaitEngine implements AsyncWaitEngine {
  @Override
  public void waitForAllOn(NotifyCallback notifyCallback, String... correlationIds) {
    // TODO
    // notifyCallback.notify(Collections.singletonMap(correlationIds[0], CVNGStep.CVNGResponseData.builder().build()));
  }
}

package io.harness.waiter;

import io.harness.tasks.ResponseData;

public class WaitNotifyEngineV2 extends WaitNotifyEngine {
  public String progressUpdate(String correlationId, ResponseData response) {
    return correlationId;
  }
}

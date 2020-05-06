package io.harness.engine.resume;

import io.harness.delegate.beans.ResponseData;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class EngineWaitRetryCallback implements NotifyCallback {
  @Override
  public void notify(Map<String, ResponseData> response) {
    logger.info("Retry Callback Received");
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.info("Retry Error Callback Received");
  }
}

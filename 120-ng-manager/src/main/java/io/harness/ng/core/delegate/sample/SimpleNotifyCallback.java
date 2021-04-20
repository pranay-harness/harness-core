package io.harness.ng.core.delegate.sample;

import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleNotifyCallback implements OldNotifyCallback {
  @Override
  public void notify(Map<String, ResponseData> response) {
    log.info("received response = [{}]", response);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    log.error("error : [{}]", response);
  }
}

package io.harness.ng.core.delegate.sample;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class SimpleNotifyCallback implements NotifyCallback {
  @Override
  public void notify(Map<String, DelegateResponseData> response) {
    logger.info("received response = [{}]", response);
  }

  @Override
  public void notifyError(Map<String, DelegateResponseData> response) {
    logger.error("error : [{}]", response);
  }
}

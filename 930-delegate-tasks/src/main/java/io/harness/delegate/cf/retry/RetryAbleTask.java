package io.harness.delegate.cf.retry;

import io.harness.pcf.PivotalClientApiException;

public interface RetryAbleTask {
  void execute() throws PivotalClientApiException;
}

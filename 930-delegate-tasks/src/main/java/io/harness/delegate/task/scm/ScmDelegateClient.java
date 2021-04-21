package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.Callable;

@OwnedBy(HarnessTeam.DX)
public interface ScmDelegateClient {
  <T> T processScmRequest(Callable<T> callable);
}

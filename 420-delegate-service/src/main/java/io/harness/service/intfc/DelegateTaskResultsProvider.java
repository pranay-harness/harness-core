package io.harness.service.intfc;

public interface DelegateTaskResultsProvider {
  byte[] getDelegateTaskResults(String delegateTaskId);
  void destroy();
}

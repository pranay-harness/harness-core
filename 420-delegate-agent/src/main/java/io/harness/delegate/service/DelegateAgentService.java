package io.harness.delegate.service;

public interface DelegateAgentService {
  void run(boolean watched);

  void pause();

  void stop();

  void freeze();

  boolean isClientToolsInstallationFinished();

  boolean isHeartbeatHealthy();

  boolean isSocketHealthy();
}

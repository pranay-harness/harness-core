package io.harness.event.reconciliation.service;

import com.google.inject.Singleton;
import io.harness.manage.ManagedScheduledExecutorService;

@Singleton
public class DeploymentReconExecutorService extends ManagedScheduledExecutorService {
  public DeploymentReconExecutorService() {
    super("DeploymentRecon");
  }
}

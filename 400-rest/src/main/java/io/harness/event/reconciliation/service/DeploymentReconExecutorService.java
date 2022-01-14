package io.harness.event.reconciliation.service;

import io.harness.manage.ManagedScheduledExecutorService;

import com.google.inject.Singleton;

@Singleton
public class DeploymentReconExecutorService extends ManagedScheduledExecutorService {
  public DeploymentReconExecutorService() {
    super("DeploymentRecon");
  }
}

package io.harness.event.reconciliation.service;

import io.harness.event.reconciliation.deployment.ReconciliationStatus;

public interface DeploymentReconService {
  ReconciliationStatus performReconciliation(String accountId, long durationStartTs, long durationEndTs);
}

package io.harness.dms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlertReconciliation;
import software.wings.scheduler.AlertCheckJobInternal;
import software.wings.service.intfc.AssignDelegateService;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.DEL)
public class DmsProxyManagerModeImpl implements DmsProxy {
  @Inject AlertCheckJobInternal alertCheckJobInternal;
  @Inject AssignDelegateService assignDelegateService;

  @Override
  public void alertCheckJobExecute(String accountId) {
    alertCheckJobInternal.executeInternal(accountId);
  }

  @Override
  public boolean alertReconCanAssign(Alert alert) {
    NoEligibleDelegatesAlert data = (NoEligibleDelegatesAlert) alert.getAlertData();
    NoEligibleDelegatesAlertReconciliation alertReconciliation =
        (NoEligibleDelegatesAlertReconciliation) alert.getAlertReconciliation();

    return alertReconciliation.getDelegates().stream().anyMatch(delegateId
        -> assignDelegateService.canAssign(null, delegateId, alert.getAccountId(), data.getAppId(), data.getEnvId(),
            data.getInfraMappingId(), data.getTaskGroup(), data.getExecutionCapabilities(), null));
  }
}

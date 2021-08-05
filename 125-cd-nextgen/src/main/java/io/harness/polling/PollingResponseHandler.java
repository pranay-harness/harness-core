package io.harness.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.polling.ManifestPollingResponse;
import io.harness.delegate.beans.polling.PollingDelegateResponse;
import io.harness.delegate.beans.polling.PollingResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.polling.bean.ManifestPolledResponse;
import io.harness.polling.bean.PolledResponse;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.service.intfc.PollingPerpetualTaskService;
import io.harness.polling.service.intfc.PollingService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDC)
public class PollingResponseHandler {
  private static final int MAX_FAILED_ATTEMPTS = 3500;
  private PollingService pollingService;
  private PollingPerpetualTaskService pollingPerpetualTaskService;

  @Inject
  public PollingResponseHandler(
      PollingService pollingService, PollingPerpetualTaskService pollingPerpetualTaskService) {
    this.pollingService = pollingService;
    this.pollingPerpetualTaskService = pollingPerpetualTaskService;
  }

  public void handlePollingResponse(
      @NotEmpty String perpetualTaskId, @NotEmpty String accountId, PollingDelegateResponse executionResponse) {
    String pollDocId = executionResponse.getPollingDocId();
    PollingDocument pollingDocument = pollingService.get(accountId, pollDocId);
    if (pollingDocument == null || !perpetualTaskId.equals(pollingDocument.getPerpetualTaskId())) {
      pollingPerpetualTaskService.deletePerpetualTask(perpetualTaskId, accountId);
      return;
    }
    if (EmptyPredicate.isEmpty(pollingDocument.getSignature())) {
      pollingService.delete(pollingDocument);
      return;
    }
    if (executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      handleSuccessResponse(pollingDocument, executionResponse.getPollingResponse());
    } else {
      handleFailureResponse(pollingDocument, executionResponse.getPollingResponse());
    }
  }

  private void handleSuccessResponse(PollingDocument pollingDocument, PollingResponse pollingResponse) {
    String accountId = pollingDocument.getAccountId();
    String pollDocId = pollingDocument.getUuid();
    pollingService.updateFailedAttempts(accountId, pollDocId, 0);

    switch (pollingDocument.getPollingType()) {
      case MANIFEST:
        PolledResponse polledResponse = pollingDocument.getPolledResponse();
        List<String> newVersions = handleManifestResponse(accountId, pollDocId, polledResponse, pollingResponse);
        if (newVersions == null) {
          return;
        }
        break;
      case ARTIFACT:
      default:
        throw new InvalidRequestException(
            "Not implemented yet for " + pollingDocument.getPollingType() + " polling type");
    }
  }

  private void handleFailureResponse(PollingDocument pollingDocument, PollingResponse pollingResponse) {
    int failedCount = pollingDocument.getFailedAttempts() + 1;

    if (failedCount % 25 == 0) {
      pollingPerpetualTaskService.resetPerpetualTask(pollingDocument);
    }

    pollingService.updateFailedAttempts(pollingDocument.getAccountId(), pollingDocument.getUuid(), failedCount);

    if (failedCount == MAX_FAILED_ATTEMPTS) {
      pollingPerpetualTaskService.deletePerpetualTask(
          pollingDocument.getPerpetualTaskId(), pollingDocument.getAccountId());
    }
  }

  public List<String> handleManifestResponse(
      String accountId, String pollDocId, PolledResponse polledResponse, PollingResponse pollingResponse) {
    ManifestPollingResponse response = (ManifestPollingResponse) pollingResponse;
    ManifestPolledResponse savedResponse = (ManifestPolledResponse) polledResponse;
    List<String> allVersions = response.getAllVersions();
    List<String> newVersions = response.getUnpublishedVersions();

    // If polled response is null, it means it was first time collecting output from perpetual task
    // There is no need to publish collected new versions in this case.
    if (savedResponse == null) {
      pollingService.updatePolledResponse(
          accountId, pollDocId, ManifestPolledResponse.builder().allPolledKeys(new HashSet<>(newVersions)).build());
      return null;
    }

    // find if there are any new versions which are not in db. This is required because of delegate rebalancing,
    // delegate might have lose context of latest versions.
    Set<String> savedVersions = savedResponse.getAllPolledKeys();
    newVersions = newVersions.stream().filter(version -> !savedVersions.contains(version)).collect(Collectors.toList());

    pollingService.updatePolledResponse(
        accountId, pollDocId, ManifestPolledResponse.builder().allPolledKeys(new HashSet<>(allVersions)).build());
    return newVersions;
  }
}

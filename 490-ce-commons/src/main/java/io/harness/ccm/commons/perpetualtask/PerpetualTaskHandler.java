package io.harness.ccm.commons.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.commons.service.intf.CCMMetadataService;
import io.harness.exception.InvalidRequestException;

import software.wings.features.CeClusterFeature;
import software.wings.features.api.UsageLimitedFeature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class PerpetualTaskHandler {
  @Inject @Named(CeClusterFeature.FEATURE_NAME) private UsageLimitedFeature ceClusterFeature;
  @Inject private CCMMetadataService ccmMetadataService;

  public boolean onUpserted(ClusterRecord clusterRecord) {
    if (isEmpty(clusterRecord.getPerpetualTaskIds())) {
      if (ccmMetadataService.isCloudCostEnabled(clusterRecord)) {
        String accountId = clusterRecord.getAccountId();
        int maxClustersAllowed = ceClusterFeature.getMaxUsageAllowedForAccount(accountId);
        int currentClusterCount = ceClusterFeature.getUsage(accountId);

        if (currentClusterCount >= maxClustersAllowed) {
          log.info("Did not add perpetual task to cluster: '{}' for account ID {} because usage limit exceeded",
              clusterRecord.getClusterName(), accountId);
          throw new InvalidRequestException(String.format(
              "Cannot add perpetual task to cluster. Max Clusters allowed for trial: %d", maxClustersAllowed));
        }
        // TODO: cePerpetualTaskManager.createPerpetualTasks(clusterRecord);
      }
    }
    return true;
  }

  public void onDeleting(ClusterRecord clusterRecord) {
    // TODO: cePerpetualTaskManager.deletePerpetualTasks(clusterRecord);
  }

  public void onDeactivating(ClusterRecord clusterRecord) {
    // TODO: cePerpetualTaskManager.deletePerpetualTasks(clusterRecord);
  }
}

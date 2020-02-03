package io.harness.ccm.health;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ccm.CCMSettingService;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HealthStatusServiceImpl implements HealthStatusService {
  public static final Long PERPETUAL_TASK_RECENCY_THRESHOLD = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
  static final String IDENTIFIER_CLUSTER_ID_ATTRIBUTE_NAME = "identifier_clusterId";

  @Inject SettingsService settingsService;
  @Inject CCMSettingService ccmSettingService;
  @Inject ClusterRecordService clusterRecordService;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject DelegateService delegateService;
  @Inject LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  @Override
  public CEHealthStatus getHealthStatus(String cloudProviderId) {
    SettingAttribute cloudProvider = settingsService.get(cloudProviderId);
    Preconditions.checkNotNull(cloudProvider);
    Preconditions.checkArgument(ccmSettingService.isCloudCostEnabled(cloudProvider),
        format("The cloud provider with id=%s has CE disabled.", cloudProvider.getUuid()));

    List<ClusterRecord> clusterRecords = clusterRecordService.list(cloudProvider.getAccountId(), cloudProviderId);

    if (clusterRecords.isEmpty()) {
      return CEHealthStatus.builder().isHealthy(true).build();
    }

    Map<String, List<String>> clusterErrorMap = new HashMap<>();

    for (ClusterRecord clusterRecord : clusterRecords) {
      List<String> errors = getErrors(clusterRecord);
      clusterErrorMap.put(clusterRecord.getUuid(), errors);
    }

    boolean isHealthy = clusterErrorMap.values().stream().allMatch(List::isEmpty);

    List<CEClusterHealth> ceClusterHealthList = new ArrayList<>();
    for (ClusterRecord clusterRecord : clusterRecords) {
      ceClusterHealthList.add(getClusterHealth(clusterRecord));
    }

    return CEHealthStatus.builder().isHealthy(isHealthy).ceClusterHealthList(ceClusterHealthList).build();
  }

  private CEClusterHealth getClusterHealth(ClusterRecord clusterRecord) {
    return CEClusterHealth.builder()
        .clusterId(clusterRecord.getUuid())
        .clusterRecord(clusterRecord)
        .errors(getErrors(clusterRecord))
        .lastEventTimestamp(getLastEventTimestamp(clusterRecord.getAccountId(), IDENTIFIER_CLUSTER_ID_ATTRIBUTE_NAME))
        .build();
  }

  private long getLastEventTimestamp(String accountId, String identifier) {
    LastReceivedPublishedMessage lastReceivedPublishedMessage =
        lastReceivedPublishedMessageDao.get(accountId, identifier);
    if (lastReceivedPublishedMessage == null) {
      return 0;
    }
    return lastReceivedPublishedMessage.getLastReceivedAt();
  }

  private List<String> getErrors(ClusterRecord clusterRecord) {
    List<String> errorsMessages = new ArrayList<>();
    if (null == clusterRecord.getPerpetualTaskIds()) {
      errorsMessages.add(String.format(CEError.PERPETUAL_TASK_CREATION_FAILURE.getMessage(), clusterRecord.getUuid()));
      return errorsMessages;
    }

    List<String> perpetualTaskIds = Arrays.asList(clusterRecord.getPerpetualTaskIds());
    for (String taskId : perpetualTaskIds) {
      PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
      String delegateId = perpetualTaskRecord.getDelegateId();
      if (isNullOrEmpty(delegateId)) {
        errorsMessages.add(String.format(CEError.PERPETUAL_TASK_NOT_ASSIGNED.getMessage(), clusterRecord.getUuid()));
        continue;
      }

      if (!delegateService.isDelegateConnected(delegateId)) {
        errorsMessages.add(String.format(CEError.DELEGATE_NOT_AVAILABLE.getMessage(), clusterRecord.getUuid()));
        continue;
      }

      if (!hasRecentHeartbeat(perpetualTaskRecord.getLastHeartbeat())) {
        errorsMessages.add(
            String.format(CEError.PERPETUAL_TASK_MISSING_HEARTBEAT.getMessage(), clusterRecord.getUuid()));
        continue;
      }
    }
    return errorsMessages;
  }

  private boolean hasRecentHeartbeat(long lastHeartbeat) {
    return (Instant.now().toEpochMilli() - lastHeartbeat) < PERPETUAL_TASK_RECENCY_THRESHOLD;
  }
}

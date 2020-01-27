package io.harness.ccm.health;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ccm.CCMSettingService;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.health.CEHealthStatus.CEHealthStatusBuilder;
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
  public static final Long PERPETUAL_TASK_RECENCY_THRESHOLD =
      TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS); // a few hours?

  @Inject SettingsService settingsService;
  @Inject CCMSettingService ccmSettingService;
  @Inject ClusterRecordService clusterRecordService;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject DelegateService delegateService;

  @Override
  public CEHealthStatus getHealthStatus(String cloudProviderId) {
    SettingAttribute cloudProvider = settingsService.get(cloudProviderId);
    Preconditions.checkNotNull(cloudProvider);
    Preconditions.checkArgument(ccmSettingService.isCloudCostEnabled(cloudProvider),
        format("The cloud provider with id=%s has CE disabled.", cloudProvider.getUuid()));

    CEHealthStatusBuilder builder = CEHealthStatus.builder();

    List<ClusterRecord> clusterRecords = clusterRecordService.list(cloudProvider.getAccountId(), cloudProviderId);

    if (clusterRecords.isEmpty()) {
      return builder.isHealthy(true).build();
    }

    List<String> clusterIds = new ArrayList<>();
    for (ClusterRecord clusterRecord : clusterRecords) {
      clusterIds.add(clusterRecord.getUuid());
    }

    Map<String, List<String>> clusterErrorMap = new HashMap<>();

    for (ClusterRecord clusterRecord : clusterRecords) {
      List<String> errors = getErrors(clusterRecord);
      clusterErrorMap.put(clusterRecord.getUuid(), errors);
    }

    boolean isHealthy = clusterErrorMap.values().stream().allMatch(List::isEmpty);

    return builder.isHealthy(isHealthy)
        .clusterIds(clusterIds)
        .clusterRecords(clusterRecords)
        .clusterErrors(clusterErrorMap)
        .build();
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

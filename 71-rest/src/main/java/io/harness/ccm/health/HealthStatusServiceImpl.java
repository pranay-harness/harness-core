package io.harness.ccm.health;

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

    Map<String, List<CEError>> clusterErrorMap = new HashMap<>();

    for (ClusterRecord clusterRecord : clusterRecords) {
      clusterErrorMap.put(clusterRecord.getUuid(), getErrors(clusterRecord));
    }

    boolean isHealthy = clusterErrorMap.values().stream().allMatch(List::isEmpty);

    return builder.isHealthy(isHealthy)
        .clusterIds(clusterIds)
        .clusterRecords(clusterRecords)
        .clusterErrors(clusterErrorMap)
        .build();
  }

  private List<CEError> getErrors(ClusterRecord clusterRecord) {
    if (null == clusterRecord.getPerpetualTaskIds()) {
      return new ArrayList<>(Arrays.asList(CEError.PERPETUAL_TASK_CREATION_FAILURE));
    }

    List<CEError> errors = new ArrayList<>();
    List<String> perpetualTaskIds = Arrays.asList(clusterRecord.getPerpetualTaskIds());
    for (String taskId : perpetualTaskIds) {
      PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
      if (!hasRecentHeartbeat(perpetualTaskRecord.getLastHeartbeat())) {
        errors.add(CEError.PERPETUAL_TASK_MISSING_HEARTBEAT);
      }
    }
    return errors;
  }

  private boolean hasRecentHeartbeat(long lastHeartbeat) {
    return (Instant.now().toEpochMilli() - lastHeartbeat) < PERPETUAL_TASK_RECENCY_THRESHOLD;
  }
}

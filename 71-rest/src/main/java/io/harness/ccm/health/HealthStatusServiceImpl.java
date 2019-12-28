package io.harness.ccm.health;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    Preconditions.checkArgument(ccmSettingService.isCloudCostEnabled(cloudProvider));

    CEHealthStatusBuilder builder = CEHealthStatus.builder();

    List<ClusterRecord> clusterRecords = clusterRecordService.list(cloudProvider.getAccountId(), cloudProviderId);

    if (clusterRecords.isEmpty()) {
      return builder.isHealthy(true).build();
    }

    List<String> perpetualTaskIds = new ArrayList<>();
    for (ClusterRecord clusterRecord : clusterRecords) {
      perpetualTaskIds =
          Stream.concat(Arrays.asList(clusterRecord.getPerpetualTaskIds()).stream(), perpetualTaskIds.stream())
              .collect(Collectors.toList());
    }

    if (perpetualTaskIds.isEmpty()) {
      return builder.isHealthy(false).build();
    }

    Map<String, List<CEError>> taskErrorMap = new HashMap<>();
    for (String taskId : perpetualTaskIds) {
      PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
      taskErrorMap.put(perpetualTaskRecord.getUuid(), new ArrayList<>());
      if (!hasRecentHeartbeat(perpetualTaskRecord.getLastHeartbeat())) {
        List<CEError> errors = taskErrorMap.get(perpetualTaskRecord.getUuid());
        errors.add(CEError.PERPETUAL_TASK_MISSING_HEARTBEAT);
        taskErrorMap.put(perpetualTaskRecord.getUuid(), errors);
      }
    }
    boolean isHealthy = taskErrorMap.values().stream().allMatch(x -> x.isEmpty());

    return builder.isHealthy(isHealthy && perpetualTaskIds.size() >= clusterRecords.size())
        .clusterRecords(clusterRecords)
        .taskErrorMap(taskErrorMap)
        .build();
  }

  private boolean hasRecentHeartbeat(long lastHeartbeat) {
    return (Instant.now().toEpochMilli() - lastHeartbeat) < PERPETUAL_TASK_RECENCY_THRESHOLD;
  }
}

package io.harness.ccm.health;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.harness.ccm.health.CEError.DELEGATE_NOT_AVAILABLE;
import static io.harness.ccm.health.CEError.METRICS_SERVER_NOT_FOUND;
import static io.harness.ccm.health.CEError.NO_ELIGIBLE_DELEGATE;
import static io.harness.ccm.health.CEError.NO_RECENT_EVENTS_PUBLISHED;
import static io.harness.ccm.health.CEError.PERPETUAL_TASK_CREATION_FAILURE;
import static io.harness.ccm.health.CEError.PERPETUAL_TASK_NOT_ASSIGNED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.ccm.config.CCMSettingService;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HealthStatusServiceImpl implements HealthStatusService {
  @Inject SettingsService settingsService;
  @Inject CCMSettingService ccmSettingService;
  @Inject ClusterRecordService clusterRecordService;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;
  @Inject CeExceptionRecordDao ceExceptionRecordDao;

  private static final String LAST_EVENT_TIMESTAMP_MESSAGE = "Last event collected at %s";
  public static final Long EVENT_TIMESTAMP_RECENCY_THRESHOLD = TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES);

  @Override
  public CEHealthStatus getHealthStatus(String cloudProviderId) {
    return getHealthStatus(cloudProviderId, true);
  }

  @Override
  public CEHealthStatus getHealthStatus(String cloudProviderId, boolean cloudCostEnabled) {
    SettingAttribute cloudProvider = settingsService.get(cloudProviderId);
    Preconditions.checkNotNull(cloudProvider);
    if (cloudCostEnabled) {
      Preconditions.checkArgument(ccmSettingService.isCloudCostEnabled(cloudProvider),
          format("The cloud provider with id=%s has CE disabled.", cloudProvider.getUuid()));
    }

    List<ClusterRecord> clusterRecords = clusterRecordService.list(cloudProvider.getAccountId(), null, cloudProviderId);

    if (clusterRecords.isEmpty()) {
      return CEHealthStatus.builder().isHealthy(true).build();
    }

    List<CEClusterHealth> ceClusterHealthList = new ArrayList<>();
    for (ClusterRecord clusterRecord : clusterRecords) {
      ceClusterHealthList.add(getClusterHealth(clusterRecord));
    }

    boolean isHealthy = ceClusterHealthList.stream().allMatch(CEClusterHealth::isHealthy);
    return CEHealthStatus.builder().isHealthy(isHealthy).ceClusterHealthList(ceClusterHealthList).build();
  }

  private CEClusterHealth getClusterHealth(ClusterRecord clusterRecord) {
    List<CEError> errors = getErrors(clusterRecord);
    return CEClusterHealth.builder()
        .isHealthy(isEmpty(errors))
        .clusterId(clusterRecord.getUuid())
        .clusterRecord(clusterRecord)
        .messages(getMessages(clusterRecord, errors))
        .lastEventTimestamp(getLastEventTimestamp(clusterRecord.getAccountId(), clusterRecord.getUuid()))
        .build();
  }

  private List<CEError> getErrors(ClusterRecord clusterRecord) {
    List<CEError> errors = new ArrayList<>();
    if (null == clusterRecord.getPerpetualTaskIds()) {
      errors.add(PERPETUAL_TASK_CREATION_FAILURE);
      return errors;
    }
    List<String> perpetualTaskIds = Arrays.asList(clusterRecord.getPerpetualTaskIds());
    for (String taskId : perpetualTaskIds) {
      PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
      String delegateId = perpetualTaskRecord.getDelegateId();
      if (isNullOrEmpty(delegateId)) {
        if (perpetualTaskRecord.getState().equals(PerpetualTaskState.TASK_UNASSIGNED.name())) {
          errors.add(PERPETUAL_TASK_NOT_ASSIGNED);
        } else if (perpetualTaskRecord.getState().equals(PerpetualTaskState.NO_DELEGATE_AVAILABLE.name())) {
          errors.add(DELEGATE_NOT_AVAILABLE);
        } else if (perpetualTaskRecord.getState().equals(PerpetualTaskState.NO_ELIGIBLE_DELEGATES.name())) {
          errors.add(NO_ELIGIBLE_DELEGATE);
        }
        continue;
      }

      long lastEventTimestamp = getLastEventTimestamp(clusterRecord.getAccountId(), clusterRecord.getUuid());
      if (lastEventTimestamp != 0 && !hasRecentEvents(lastEventTimestamp)) {
        errors.add(NO_RECENT_EVENTS_PUBLISHED);
      }

      CeExceptionRecord CeExceptionRecord =
          ceExceptionRecordDao.getLatestException(clusterRecord.getAccountId(), clusterRecord.getUuid());
      if (CeExceptionRecord != null && CeExceptionRecord.getMessage().contains("metrics-server")) {
        errors.add(METRICS_SERVER_NOT_FOUND);
      }
    }

    return errors;
  }

  private List<String> getMessages(ClusterRecord clusterRecord, List<CEError> errors) {
    Preconditions.checkNotNull(clusterRecord.getCluster());

    String clusterName = clusterRecord.getCluster().getClusterName();
    List<String> messages = new ArrayList<>();
    long lastEventTimestamp = getLastEventTimestamp(clusterRecord.getAccountId(), clusterRecord.getUuid());
    for (CEError error : errors) {
      switch (error) {
        case PERPETUAL_TASK_NOT_ASSIGNED:
          messages.add(PERPETUAL_TASK_NOT_ASSIGNED.getMessage());
          break;
        case DELEGATE_NOT_AVAILABLE:
          String delegateName = getDelegateName(clusterRecord.getCluster().getCloudProviderId());
          if (delegateName == null) {
            messages.add(String.format(DELEGATE_NOT_AVAILABLE.getMessage(), ""));
          } else {
            messages.add(String.format(DELEGATE_NOT_AVAILABLE.getMessage(), "\"" + delegateName + "\""));
          }
          break;
        case NO_ELIGIBLE_DELEGATE:
          messages.add(format(NO_ELIGIBLE_DELEGATE.getMessage(), clusterName));
          break;
        case NO_RECENT_EVENTS_PUBLISHED:
          messages.add(
              String.format(NO_RECENT_EVENTS_PUBLISHED.getMessage(), clusterName, new Date(lastEventTimestamp)));
          break;
        case METRICS_SERVER_NOT_FOUND:
          messages.add(String.format(METRICS_SERVER_NOT_FOUND.getMessage(), clusterName));
          break;
        default:
          messages.add("Unexpected error. Please contact Harness support.");
          break;
      }
    }

    if (isEmpty(messages)) {
      if (lastEventTimestamp <= 0) {
        messages.add("No events received. The first event will arrive in 5 minutes.");
      } else {
        messages.add(String.format(LAST_EVENT_TIMESTAMP_MESSAGE, new Date(lastEventTimestamp)));
      }
    }
    return messages;
  }

  private String getDelegateName(String cloudProviderId) {
    String delegateName = null;
    SettingValue cloudProvider = settingsService.get(cloudProviderId).getValue();
    if (cloudProvider instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig k8sCloudProvider = (KubernetesClusterConfig) cloudProvider;
      delegateName = k8sCloudProvider.getDelegateName();
    } else if (cloudProvider instanceof AwsConfig) {
      AwsConfig awsCloudProvider = (AwsConfig) cloudProvider;
      delegateName = awsCloudProvider.getTag();
    }
    return delegateName;
  }

  private long getLastEventTimestamp(String accountId, String identifier) {
    LastReceivedPublishedMessage lastReceivedPublishedMessage =
        lastReceivedPublishedMessageDao.get(accountId, identifier);
    if (lastReceivedPublishedMessage == null) {
      return 0;
    }
    return lastReceivedPublishedMessage.getLastReceivedAt();
  }

  private boolean hasRecentEvents(long eventTimestamp) {
    return (Instant.now().toEpochMilli() - eventTimestamp) < EVENT_TIMESTAMP_RECENCY_THRESHOLD;
  }
}

package software.wings.service.impl.newrelic;

import static software.wings.beans.TaskType.NEWRELIC_COLLECT_METRIC_DATAV2;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.NewRelicDataCollector;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.MetricsDataCollectionInfo;
import software.wings.sm.StateType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NewRelicDataCollectionInfoV2 extends MetricsDataCollectionInfo implements ExecutionCapabilityDemander {
  private NewRelicConfig newRelicConfig;
  private long newRelicAppId;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(newRelicConfig, encryptedDataDetails);
  }

  @Builder
  public NewRelicDataCollectionInfoV2(String accountId, String applicationId, String envId, Instant startTime,
      Instant endTime, Set<String> hosts, String cvConfigId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String cvTaskId, NewRelicConfig newRelicConfig, long newRelicAppId,
      List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> hostsToGroupNameMap) {
    super(accountId, applicationId, envId, startTime, endTime, hosts, cvConfigId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, cvTaskId, hostsToGroupNameMap);
    this.encryptedDataDetails = encryptedDataDetails;
    this.newRelicConfig = newRelicConfig;
    this.newRelicAppId = newRelicAppId;
  }

  @Override
  public TaskType getTaskType() {
    return NEWRELIC_COLLECT_METRIC_DATAV2;
  }

  @Override
  public StateType getStateType() {
    return StateType.NEW_RELIC;
  }

  @Override
  public Class<NewRelicDataCollector> getDataCollectorImplClass() {
    return NewRelicDataCollector.class;
  }

  @Override
  public Optional<String> getUrlForValidation() {
    return Optional.of(newRelicConfig.getNewRelicUrl());
  }

  @Override
  public Optional<EncryptionConfig> getEncryptionConfig() {
    return Optional.of(getEncryptedDataDetails().get(0).getEncryptionConfig());
  }

  @Override
  public Optional<EncryptableSetting> getEncryptableSetting() {
    return Optional.of(newRelicConfig);
  }

  @Override
  public DataCollectionInfoV2 deepCopy() {
    NewRelicDataCollectionInfoV2 newRelicDataCollectionInfo =
        NewRelicDataCollectionInfoV2.builder()
            .newRelicConfig(this.newRelicConfig)
            .newRelicAppId(newRelicAppId)
            .encryptedDataDetails(new ArrayList<>(encryptedDataDetails))
            .build();
    super.copy(newRelicDataCollectionInfo);
    return newRelicDataCollectionInfo;
  }
}

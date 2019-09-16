package software.wings.service.impl.splunk;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.DataCollector;
import software.wings.delegatetasks.cv.SplunkDataCollector;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.sm.StateType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class SplunkDataCollectionInfoV2 extends LogDataCollectionInfoV2 implements ExecutionCapabilityDemander {
  private SplunkConfig splunkConfig;
  private boolean isAdvancedQuery;

  @Builder
  public SplunkDataCollectionInfoV2(String accountId, String applicationId, String envId, Instant startTime,
      Instant endTime, Set<String> hosts, String cvConfigId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String query, String hostnameField,
      List<EncryptedDataDetail> encryptedDataDetails, SplunkConfig splunkConfig, boolean isAdvancedQuery) {
    super(accountId, applicationId, envId, startTime, endTime, hosts, cvConfigId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, query, hostnameField, encryptedDataDetails);
    this.splunkConfig = splunkConfig;
    this.isAdvancedQuery = isAdvancedQuery;
  }
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(splunkConfig, getEncryptedDataDetails());
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.SPLUNK_COLLECT_LOG_DATAV2;
  }

  @Override
  public StateType getStateType() {
    return StateType.SPLUNKV2;
  }

  @Override
  public Class<? extends DataCollector<?>> getDataCollectorImplClass() {
    return SplunkDataCollector.class;
  }

  @Override
  public Optional<String> getUrlForValidation() {
    return Optional.of(splunkConfig.getSplunkUrl());
  }

  @Override
  public Optional<EncryptionConfig> getEncryptionConfig() {
    return Optional.of(getEncryptedDataDetails().get(0).getEncryptionConfig());
  }

  @Override
  public Optional<EncryptableSetting> getEncryptableSetting() {
    return Optional.of(splunkConfig);
  }
}

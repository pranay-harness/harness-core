package software.wings.service.impl.elk;

import static software.wings.beans.TaskType.ELK_COLLECT_LOG_DATAV2;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.ElkConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.DataCollector;
import software.wings.delegatetasks.cv.ElkDataCollector;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.sm.StateType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class ElkDataCollectionInfoV2 extends LogDataCollectionInfoV2 {
  private ElkConfig elkConfig;
  private String indices;
  private String messageField;
  private String timestampField;
  private String timestampFieldFormat;
  private ElkQueryType queryType;
  @Builder
  public ElkDataCollectionInfoV2(String accountId, String applicationId, String envId, Instant startTime,
      Instant endTime, Set<String> hosts, String cvConfigId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String cvTaskId, String query, String hostnameField,
      List<EncryptedDataDetail> encryptedDataDetails, ElkConfig elkConfig, String indices, String messageField,
      String timestampField, String timestampFieldFormat, ElkQueryType queryType) {
    super(accountId, applicationId, envId, startTime, endTime, hosts, cvConfigId, stateExecutionId, workflowId,
        workflowExecutionId, serviceId, cvTaskId, encryptedDataDetails, query, hostnameField);
    this.elkConfig = elkConfig;
    this.indices = indices;
    this.messageField = messageField;
    this.timestampField = timestampField;
    this.timestampFieldFormat = timestampFieldFormat;
    this.queryType = queryType;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(elkConfig, getEncryptedDataDetails());
  }

  @Override
  public TaskType getTaskType() {
    return ELK_COLLECT_LOG_DATAV2;
  }

  @Override
  public StateType getStateType() {
    return StateType.ELK;
  }

  @Override
  public Class<? extends DataCollector<? extends DataCollectionInfoV2>> getDataCollectorImplClass() {
    return ElkDataCollector.class;
  }

  @Override
  public Optional<String> getUrlForValidation() {
    return Optional.of(elkConfig.getElkUrl());
  }

  @Override
  public Optional<EncryptionConfig> getEncryptionConfig() {
    return Optional.of(getEncryptedDataDetails().get(0).getEncryptionConfig());
  }

  @Override
  public Optional<EncryptableSetting> getEncryptableSetting() {
    return Optional.of(elkConfig);
  }

  @Override
  public DataCollectionInfoV2 deepCopy() {
    ElkDataCollectionInfoV2 elkDataCollectionInfo = ElkDataCollectionInfoV2.builder()
                                                        .elkConfig(elkConfig)
                                                        .indices(indices)
                                                        .messageField(messageField)
                                                        .timestampField(timestampField)
                                                        .timestampFieldFormat(timestampFieldFormat)
                                                        .queryType(queryType)
                                                        .build();
    super.copy(elkDataCollectionInfo);
    return elkDataCollectionInfo;
  }
}

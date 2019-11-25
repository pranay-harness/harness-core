package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.DataCollector;
import software.wings.sm.StateType;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DataCollectionInfoV2 implements TaskParameters {
  private String accountId;
  private String applicationId;
  private String envId;
  private Instant startTime;
  private Instant endTime;
  private Set<String> hosts;
  private String cvConfigId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvTaskId;

  public Set<String> getHosts() {
    // morphia converts empty objects to null while saving to database so making sure it's always returns empty set if
    // null.
    if (hosts == null) {
      return Collections.emptySet();
    }
    return hosts;
  }
  @JsonIgnore public abstract TaskType getTaskType();
  @JsonIgnore public abstract StateType getStateType();
  @JsonIgnore
  public abstract Class<? extends DataCollector<? extends DataCollectionInfoV2>> getDataCollectorImplClass();
  @JsonIgnore public abstract Optional<String> getUrlForValidation();

  @JsonIgnore public abstract Optional<EncryptionConfig> getEncryptionConfig();
  @JsonIgnore public abstract Optional<EncryptableSetting> getEncryptableSetting();

  public abstract List<EncryptedDataDetail> getEncryptedDataDetails();
  @JsonIgnore public abstract DataCollectionInfoV2 deepCopy();

  protected void copy(DataCollectionInfoV2 dataCollectionInfo) {
    dataCollectionInfo.setAccountId(this.accountId);
    dataCollectionInfo.setApplicationId(this.applicationId);
    dataCollectionInfo.setEnvId(this.envId);
    dataCollectionInfo.setStartTime(this.startTime);
    dataCollectionInfo.setEndTime(this.endTime);
    dataCollectionInfo.setHosts(new HashSet<>(this.hosts));
    dataCollectionInfo.setCvConfigId(this.cvConfigId);
    dataCollectionInfo.setStateExecutionId(this.stateExecutionId);
    dataCollectionInfo.setWorkflowId(this.workflowId);
    dataCollectionInfo.setWorkflowExecutionId(this.workflowExecutionId);
    dataCollectionInfo.setServiceId(this.serviceId);
  }
}

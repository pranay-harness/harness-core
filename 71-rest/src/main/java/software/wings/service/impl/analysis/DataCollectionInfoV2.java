package software.wings.service.impl.analysis;

import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.DataCollector;
import software.wings.sm.StateType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@AllArgsConstructor
public abstract class DataCollectionInfoV2 implements TaskParameters {
  private final String accountId;
  private final String applicationId;
  private final String envId;
  private Instant startTime;
  private Instant endTime;
  private Set<String> hosts;
  private final String cvConfigId;
  private final String stateExecutionId;
  private final String workflowId;
  private final String workflowExecutionId;
  private final String serviceId;

  public Set<String> getHosts() {
    // morphia converts empty objects to null while saving to database so making sure it's always returns empty set if
    // null.
    if (hosts == null) {
      return Collections.emptySet();
    }
    return hosts;
  }

  public abstract TaskType getTaskType();

  public abstract StateType getStateType();

  public abstract Class<? extends DataCollector<? extends DataCollectionInfoV2>> getDataCollectorImplClass();

  public abstract Optional<String> getUrlForValidation();

  public abstract Optional<EncryptionConfig> getEncryptionConfig();

  public abstract Optional<EncryptableSetting> getEncryptableSetting();

  public abstract List<EncryptedDataDetail> getEncryptedDataDetails();
}

package software.wings.service.impl.analysis;

import io.harness.delegate.task.TaskParameters;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Common Log Data Collection Info class containing attributes used by Log Verification providers while
 * Data collection
 * Created by rsingh on 8/8/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class LogDataCollectionInfo extends DataCollectionInfo implements TaskParameters {
  private String query;
  private long startTime;
  private long endTime;
  private int startMinute;
  private int collectionTime;
  private String hostnameField;
  private Set<String> hosts;
  private StateType stateType;
  List<EncryptedDataDetail> encryptedDataDetails;

  public LogDataCollectionInfo(String accountId, String applicationId, String stateExecutionId, String cvConfigId,
      String workflowId, String workflowExecutionId, String serviceId, String query, long startTime, long endTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts, StateType stateType,
      List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, cvConfigId, workflowId, workflowExecutionId, serviceId);
    this.query = query;
    this.startTime = startTime;
    this.endTime = endTime;
    this.startMinute = startMinute;
    this.collectionTime = collectionTime;
    this.hostnameField = hostnameField;
    this.hosts = hosts;
    this.stateType = stateType;
    this.encryptedDataDetails = encryptedDataDetails;
  }
}

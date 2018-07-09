package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 8/8/17.
 */
@Data
@AllArgsConstructor
public abstract class LogDataCollectionInfo {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String query;
  private long startTime;
  private int startMinute;
  private int collectionTime;
  private String hostnameField;
  private Set<String> hosts;
  private StateType stateType;
  List<EncryptedDataDetail> encryptedDataDetails;
}

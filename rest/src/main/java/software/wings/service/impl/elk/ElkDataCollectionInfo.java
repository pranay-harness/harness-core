package software.wings.service.impl.elk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.wings.beans.ElkConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class ElkDataCollectionInfo extends LogDataCollectionInfo {
  private ElkConfig elkConfig;
  private String indices;
  private String hostnameField;
  private String messageField;
  private String timestampField;
  private String timestampFieldFormat;

  public ElkDataCollectionInfo(ElkConfig elkConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, Set<String> queries, String indices,
      String hostnameField, String messageField, String timestampField, String timestampFieldFormat, long startTime,
      int startMinute, int collectionTime, Set<String> hosts, List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, queries, startTime,
        startMinute, collectionTime, hosts, StateType.ELK, encryptedDataDetails);
    this.elkConfig = elkConfig;
    this.indices = indices;
    this.hostnameField = hostnameField;
    this.messageField = messageField;
    this.timestampField = timestampField;
    this.timestampFieldFormat = timestampFieldFormat;
  }
}

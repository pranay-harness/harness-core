package software.wings.service.impl.elk;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@ToString(callSuper = true)
public class ElkDataCollectionInfo extends LogDataCollectionInfo {
  private ElkConfig elkConfig;
  private String indices;
  private String messageField;
  private String timestampField;
  private String timestampFieldFormat;
  private ElkQueryType queryType;

  @Builder
  public ElkDataCollectionInfo(ElkConfig elkConfig, String accountId, String applicationId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, String query, String indices,
      String messageField, String timestampField, String timestampFieldFormat, ElkQueryType queryType, long startTime,
      int startMinute, int collectionTime, String hostnameField, Set<String> hosts,
      List<EncryptedDataDetail> encryptedDataDetails) {
    super(accountId, applicationId, stateExecutionId, workflowId, workflowExecutionId, serviceId, query, startTime,
        startMinute, collectionTime, hostnameField, hosts, StateType.ELK, encryptedDataDetails);
    this.elkConfig = elkConfig;
    this.indices = indices;
    this.messageField = messageField;
    this.timestampField = timestampField;
    this.timestampFieldFormat = timestampFieldFormat;
    this.queryType = queryType;
  }
}

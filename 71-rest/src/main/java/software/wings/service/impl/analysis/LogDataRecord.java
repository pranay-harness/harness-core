package software.wings.service.impl.analysis;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsingh on 6/20/17.
 */
@Entity(value = "logDataRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("stateType")
    , @Field("stateExecutionId"), @Field("host"), @Field("timeStamp"), @Field("logMD5Hash"), @Field("clusterLevel"),
        @Field("clusterLevel"), @Field("logCollectionMinute")
  }, options = @IndexOptions(unique = true, name = "logUniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class LogDataRecord extends Base {
  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String serviceId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private String query;

  @NotEmpty private String clusterLabel;
  @NotEmpty private String host;

  @NotEmpty @Indexed private long timeStamp;

  @NotEmpty private int count;
  @NotEmpty private String logMessage;
  @NotEmpty private String logMD5Hash;
  @NotEmpty @Indexed private ClusterLevel clusterLevel;
  @NotEmpty @Indexed private int logCollectionMinute;

  public static List<LogDataRecord> generateDataRecords(StateType stateType, String applicationId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, ClusterLevel heartbeat, List<LogElement> logElements) {
    final List<LogDataRecord> records = new ArrayList<>();
    for (LogElement logElement : logElements) {
      final LogDataRecord record = new LogDataRecord();
      record.setStateType(stateType);
      record.setWorkflowId(workflowId);
      record.setWorkflowExecutionId(workflowExecutionId);
      record.setStateExecutionId(stateExecutionId);
      record.setQuery(logElement.getQuery());
      record.setAppId(applicationId);
      record.setClusterLabel(logElement.getClusterLabel());
      record.setHost(logElement.getHost());
      record.setTimeStamp(logElement.getTimeStamp());
      record.setCount(logElement.getCount());
      record.setLogMessage(logElement.getLogMessage());
      record.setLogMD5Hash(DigestUtils.md5Hex(logElement.getLogMessage()));
      record.setClusterLevel(Integer.parseInt(logElement.getClusterLabel()) < 0 ? heartbeat : clusterLevel);
      record.setServiceId(serviceId);
      record.setLogCollectionMinute(logElement.getLogCollectionMinute());

      records.add(record);
    }
    return records;
  }
}

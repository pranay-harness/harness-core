package software.wings.service.impl.splunk;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsingh on 6/20/17.
 */
@Entity(value = "splunkLogs", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("applicationId"), @Field("host"), @Field("timeStamp"), @Field("logMD5Hash")
  }, options = @IndexOptions(unique = true, name = "splunkLogUniqueIdx"))
})
@Data
public class SplunkLogDataRecord extends Base {
  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private String query;

  @NotEmpty private String applicationId;
  @NotEmpty private String clusterLabel;
  @NotEmpty private String host;

  @NotEmpty @Indexed private long timeStamp;

  @NotEmpty private int count;
  @NotEmpty private String logMessage;
  @NotEmpty private String logMD5Hash;
  @Indexed private boolean processed;
  @Indexed private int logCollectionMinute;

  public SplunkLogDataRecord() {
    // for json parsing
  }

  public SplunkLogDataRecord(String applicationId, String stateExecutionId, String workflowId, String query,
      String clusterLabel, String host, long timeStamp, int count, String logMessage, String logMD5Hash,
      boolean processed, int logCollectionMinute) {
    this.applicationId = applicationId;
    this.stateExecutionId = stateExecutionId;
    this.workflowId = workflowId;
    this.query = query;
    this.clusterLabel = clusterLabel;
    this.host = host;
    this.timeStamp = timeStamp;
    this.count = count;
    this.logMessage = logMessage;
    this.logMD5Hash = logMD5Hash;
    this.processed = processed;
    this.logCollectionMinute = logCollectionMinute;
  }

  public static List<SplunkLogDataRecord> generateDataRecords(
      String applicationId, String stateExecutionId, String workflowExecutionId, List<SplunkLogElement> logElements) {
    final List<SplunkLogDataRecord> records = new ArrayList<>();
    for (SplunkLogElement logElement : logElements) {
      records.add(new SplunkLogDataRecord(applicationId, stateExecutionId, workflowExecutionId, logElement.getQuery(),
          logElement.getClusterLabel(), logElement.getHost(), logElement.getTimeStamp(), logElement.getCount(),
          logElement.getLogMessage(), DigestUtils.md5Hex(logElement.getLogMessage()), false,
          logElement.getLogCollectionMinute()));
    }
    return records;
  }
}

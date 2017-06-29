package software.wings.service.impl.splunk;

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
public class SplunkLogDataRecord extends Base {
  @NotEmpty @Indexed private String stateExecutionId;
  @NotEmpty private String applicationId;
  @NotEmpty private String clusterLabel;
  @NotEmpty private String host;

  @NotEmpty @Indexed private long timeStamp;

  @NotEmpty private int count;
  @NotEmpty private String logMessage;
  @NotEmpty private String logMD5Hash;
  @Indexed private boolean processed;
  @Indexed private int logCollectionMinute;

  public SplunkLogDataRecord(String applicationId, String stateExecutionId, String clusterLabel, String host,
      long timeStamp, int count, String logMessage, String logMD5Hash, boolean processed, int logCollectionMinute) {
    this.applicationId = applicationId;
    this.stateExecutionId = stateExecutionId;
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
      String applicationId, String stateExecutionId, List<SplunkLogElement> logElements) {
    final List<SplunkLogDataRecord> records = new ArrayList<>();
    for (SplunkLogElement logElement : logElements) {
      records.add(new SplunkLogDataRecord(applicationId, stateExecutionId, logElement.getClusterLabel(),
          logElement.getHost(), logElement.getTimeStamp(), logElement.getCount(), logElement.getLogMessage(),
          DigestUtils.md5Hex(logElement.getLogMessage()), false, logElement.getLogCollectionMinute()));
    }
    return records;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getStateExecutionId() {
    return stateExecutionId;
  }

  public void setStateExecutionId(String stateExecutionId) {
    this.stateExecutionId = stateExecutionId;
  }

  public String getClusterLabel() {
    return clusterLabel;
  }

  public void setClusterLabel(String clusterLabel) {
    this.clusterLabel = clusterLabel;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  public String getLogMD5Hash() {
    return logMD5Hash;
  }

  public void setLogMD5Hash(String logMD5Hash) {
    this.logMD5Hash = logMD5Hash;
  }

  public boolean isProcessed() {
    return processed;
  }

  public void setProcessed(boolean processed) {
    this.processed = processed;
  }

  public int getLogCollectionMinute() {
    return logCollectionMinute;
  }

  public void setLogCollectionMinute(int logCollectionMinute) {
    this.logCollectionMinute = logCollectionMinute;
  }
}

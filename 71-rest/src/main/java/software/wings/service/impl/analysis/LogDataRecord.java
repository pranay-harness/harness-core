package software.wings.service.impl.analysis;

import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by rsingh on 6/20/17.
 */
@Entity(value = "logDataRecords", noClassnameStored = true)
@Indexes({
  @Index(fields =
      {
        @Field("stateType")
        , @Field("stateExecutionId"), @Field("host"), @Field("timeStamp"), @Field("logMD5Hash"), @Field("clusterLevel"),
            @Field("clusterLevel"), @Field("logCollectionMinute")
      },
      options = @IndexOptions(unique = true, name = "logUniqueIdx"))
  ,
      @Index(fields = {
        @Field("cvConfigId"), @Field("logCollectionMinute")
      }, options = @IndexOptions(unique = false, name = "cvLogsIdx"))
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil", "logMessage"})
public class LogDataRecord extends Base {
  @NotEmpty private StateType stateType;

  @NotEmpty private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty private String serviceId;

  @NotEmpty @Indexed private String stateExecutionId;

  @Indexed private String cvConfigId;

  @NotEmpty private String query;

  @NotEmpty private String clusterLabel;
  @NotEmpty private String host;

  @NotEmpty private long timeStamp;

  private int timesLabeled;
  @NotEmpty private int count;
  @NotEmpty private String logMessage;
  @NotEmpty private String logMD5Hash;
  @NotEmpty private ClusterLevel clusterLevel;
  @NotEmpty private long logCollectionMinute;

  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  public static List<LogDataRecord> generateDataRecords(StateType stateType, String applicationId, String cvConfigId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, ClusterLevel heartbeat, List<LogElement> logElements) {
    final List<LogDataRecord> records = new ArrayList<>();
    for (LogElement logElement : logElements) {
      final LogDataRecord record = new LogDataRecord();
      record.setStateType(stateType);
      record.setWorkflowId(workflowId);
      record.setWorkflowExecutionId(workflowExecutionId);
      record.setCvConfigId(cvConfigId);
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

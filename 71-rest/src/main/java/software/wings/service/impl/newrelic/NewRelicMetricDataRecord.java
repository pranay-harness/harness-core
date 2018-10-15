package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "newRelicMetricRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("name")
    , @Field("host"), @Field("timeStamp"), @Field("workflowExecutionId"), @Field("stateExecutionId"),
        @Field("serviceId"), @Field("workflowId"), @Field("level"), @Field("stateType"), @Field("groupName")
  }, options = @IndexOptions(unique = true, name = "metricUniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicMetricDataRecord extends Base {
  @Transient public static String DEFAULT_GROUP_NAME = "default";

  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String name;

  @Indexed private String workflowId;

  @Indexed private String workflowExecutionId;

  @Indexed private String serviceId;

  @Indexed private String cvConfigId;

  @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private long timeStamp;

  @Indexed private int dataCollectionMinute;

  @NotEmpty private String host;

  @Indexed private ClusterLevel level;

  private String tag;

  @Indexed private String groupName = DEFAULT_GROUP_NAME;

  private Map<String, Double> values = new HashMap<>();

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  @Builder
  public NewRelicMetricDataRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, StateType stateType,
      String name, String workflowId, String workflowExecutionId, String serviceId, String cvConfigId,
      String stateExecutionId, long timeStamp, int dataCollectionMinute, String host, ClusterLevel level, String tag,
      String groupName, Map<String, Double> values) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateType = stateType;
    this.name = name;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.serviceId = serviceId;
    this.cvConfigId = cvConfigId;
    this.stateExecutionId = stateExecutionId;
    this.timeStamp = timeStamp;
    this.dataCollectionMinute = dataCollectionMinute;
    this.host = host;
    this.level = level;
    this.tag = tag;
    this.groupName = isEmpty(groupName) ? DEFAULT_GROUP_NAME : groupName;
    this.values = isEmpty(values) ? new HashMap<>() : values;
    this.validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
  }
}

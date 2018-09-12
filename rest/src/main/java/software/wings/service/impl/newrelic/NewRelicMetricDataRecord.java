package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
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
@EqualsAndHashCode(callSuper = false)
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

  @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private long timeStamp;

  @Indexed private int dataCollectionMinute;

  @NotEmpty private String host;

  @Indexed private ClusterLevel level;

  private String tag;

  @Default @Indexed private String groupName = DEFAULT_GROUP_NAME;

  // generic values
  @Default private Map<String, Double> values = new HashMap<>();

  @Builder
  public NewRelicMetricDataRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, StateType stateType,
      String name, String workflowId, String workflowExecutionId, String serviceId, String stateExecutionId,
      long timeStamp, int dataCollectionMinute, String host, ClusterLevel level, String tag, String groupName,
      Map<String, Double> values) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.stateType = stateType;
    this.name = name;
    this.workflowId = workflowId;
    this.workflowExecutionId = workflowExecutionId;
    this.serviceId = serviceId;
    this.stateExecutionId = stateExecutionId;
    this.timeStamp = timeStamp;
    this.dataCollectionMinute = dataCollectionMinute;
    this.host = host;
    this.level = level;
    this.tag = tag;
    this.groupName = groupName;
    this.values = values;
  }
}

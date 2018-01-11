package software.wings.service.impl.newrelic;

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
import software.wings.beans.Base;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "newRelicMetricRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("name")
    , @Field("host"), @Field("timeStamp"), @Field("workflowExecutionId"), @Field("stateExecutionId"),
        @Field("serviceId"), @Field("workflowId"), @Field("level"), @Field("stateType")
  }, options = @IndexOptions(unique = true, name = "metricUniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewRelicMetricDataRecord extends Base {
  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String name;

  @NotEmpty @Indexed private String applicationId;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String serviceId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private long timeStamp;

  @Indexed private int dataCollectionMinute;

  @NotEmpty private String host;

  @Indexed private ClusterLevel level;

  private double error = -1;

  // new relic metrics
  private double throughput = -1;
  private double averageResponseTime = -1;
  private double apdexScore = -1;
  private long callCount;
  private double requestsPerMinute = -1;
  // appdynamics metrics
  private double response95th = -1;
  private double stalls = -1;
  private double slowCalls = -1;
}

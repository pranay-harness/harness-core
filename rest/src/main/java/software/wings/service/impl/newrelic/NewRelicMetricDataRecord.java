package software.wings.service.impl.newrelic;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "newRelicMetricRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("metricType"), @Field("host"), @Field("timeStamp"), @Field("workflowExecutionId"), @Field("stateExecutionId")
  }, options = @IndexOptions(unique = true, name = "metricUniqueIdx"))
})
@Data
@NoArgsConstructor
public class NewRelicMetricDataRecord extends Base {
  @NotEmpty @Indexed private NewRelicMetricType metricType;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String serviceId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private long timeStamp;

  @NotEmpty private String host;

  private NewRelicApdex apdexValue;

  private NewRelicErrors errors;

  private NewRelicWebTransactions webTransactions;
}

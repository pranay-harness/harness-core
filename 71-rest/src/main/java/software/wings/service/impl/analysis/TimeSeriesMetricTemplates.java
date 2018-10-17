package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.sm.StateType;

import java.util.Map;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "timeSeriesMetricTemplates", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("stateType"), @Field("stateExecutionId"), @Field("cvConfigId"), @Field("accountId")
  }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class TimeSeriesMetricTemplates extends Base {
  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String stateExecutionId;

  @Indexed private String cvConfigId;

  @Indexed private String accountId;

  @NotEmpty private Map<String, TimeSeriesMetricDefinition> metricTemplates;
}

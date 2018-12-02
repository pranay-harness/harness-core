package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.sm.StateType;

@Entity(value = "timeseriesTransactionThresholds", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("appId")
    , @Field("serviceId"), @Field("stateType"), @Field("groupName"), @Field("transactionName"), @Field("metricName"),
        @Field("cvConfigId")
  }, options = @IndexOptions(unique = true, name = "timeseriesThresholdsUniqueNewIdx"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class TimeSeriesMLTransactionThresholds extends Base {
  @NotEmpty private String serviceId;

  @NotEmpty private StateType stateType;

  @NotEmpty private String groupName;

  @NotEmpty private String transactionName;

  @NotEmpty private String metricName;

  @NotEmpty private String cvConfigId;

  TimeSeriesMetricDefinition thresholds;

  private int version;
}

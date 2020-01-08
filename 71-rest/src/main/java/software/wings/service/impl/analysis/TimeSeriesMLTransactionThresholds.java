package software.wings.service.impl.analysis;

import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.TimeSeriesCustomThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.sm.StateType;

@Indexes({
  @Index(fields = {
    @Field("appId")
    , @Field("serviceId"), @Field("stateType"), @Field("groupName"), @Field("transactionName"), @Field("metricName"),
        @Field("cvConfigId"), @Field("thresholdType")
  }, options = @IndexOptions(unique = false, name = "timeseriesThresholdsQueryIndex"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "TimeSeriesMLTransactionThresholdKeys")
@Entity(value = "timeseriesTransactionThresholds", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesMLTransactionThresholds extends Base {
  @NotEmpty private String serviceId;

  @NotEmpty private String workflowId;

  @NotEmpty private StateType stateType;

  @NotEmpty private String groupName;

  @NotEmpty private String transactionName;

  @NotEmpty private String metricName;

  @NotEmpty private String cvConfigId;

  TimeSeriesMetricDefinition thresholds;

  TimeSeriesCustomThresholdType thresholdType = TimeSeriesCustomThresholdType.ACCEPTABLE;

  private int version;
}

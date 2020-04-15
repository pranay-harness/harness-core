package software.wings.service.impl.analysis;

import com.google.common.collect.Sets;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.TimeSeriesCustomThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.HashMap;

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
public class TimeSeriesMLTransactionThresholds extends Base implements AccountAccess {
  @NotEmpty private String serviceId;

  @NotEmpty private String workflowId;

  @NotEmpty private StateType stateType;

  @NotEmpty private String groupName;

  @NotEmpty private String transactionName;

  @NotEmpty private String metricName;

  @NotEmpty private String cvConfigId;

  @Indexed private String accountId;

  TimeSeriesMetricDefinition thresholds;

  TimeSeriesCustomThresholdType thresholdType = TimeSeriesCustomThresholdType.ACCEPTABLE;

  private String customThresholdRefId;

  private int version;

  public TimeSeriesMLTransactionThresholds cloneWithoutCustomThresholds() {
    return TimeSeriesMLTransactionThresholds.builder()
        .serviceId(serviceId)
        .workflowId(workflowId)
        .stateType(stateType)
        .groupName(groupName)
        .transactionName(transactionName)
        .metricName(metricName)
        .cvConfigId(cvConfigId)
        .accountId(accountId)
        .thresholds(TimeSeriesMetricDefinition.builder()
                        .metricName(thresholds.getMetricName())
                        .metricType(thresholds.getMetricType())
                        .tags(thresholds.getTags() != null ? Sets.newHashSet(thresholds.getTags()) : null)
                        .categorizedThresholds(thresholds.getCategorizedThresholds() != null
                                ? new HashMap<>(thresholds.getCategorizedThresholds())
                                : null)
                        .customThresholds(new ArrayList<>())
                        .build())
        .build();
  }
}

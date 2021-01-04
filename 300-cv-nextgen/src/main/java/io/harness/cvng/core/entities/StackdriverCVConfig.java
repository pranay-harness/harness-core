package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition;
import io.harness.cvng.core.beans.StackdriverDefinition;
import io.harness.cvng.core.entities.MetricPack.MetricDefinition;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("STACKDRIVER")
@Data
@Builder
@FieldNameConstants(innerTypeName = "StackdriverCVConfigKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StackdriverCVConfig extends MetricCVConfig {
  private List<MetricInfo> metricInfoList;
  private String dashboardName;
  private String dashboardPath;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "MetricInfoKeys")
  public static class MetricInfo {
    private String metricName;
    private String jsonMetricDefinition;
    private List<String> tags;
    private TimeSeriesMetricType metricType;
    boolean isManualQuery;
  }

  @Override
  public DataSourceType getType() {
    return DataSourceType.STACKDRIVER;
  }

  @Override
  @JsonIgnore
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }

  @Override
  protected void validateParams() {
    checkNotNull(metricInfoList, generateErrorMessageFromParam(StackdriverCVConfigKeys.metricInfoList));

    metricInfoList.forEach(metricInfo -> {
      StackDriverMetricDefinition.extractFromJson(metricInfo.getJsonMetricDefinition());
      checkNotNull(metricInfo.getMetricType(), generateErrorMessageFromParam("metricType"));
    });
  }

  public Set<TimeSeriesThreshold> getThresholdsToCreateOnSave(
      String metricName, TimeSeriesMetricType metricType, List<TimeSeriesThresholdType> thresholdTypes) {
    Set<TimeSeriesThreshold> thresholds = new HashSet<>();
    metricType.getThresholds().forEach(threshold -> {
      thresholdTypes.forEach(type -> {
        Gson gson = new Gson();
        TimeSeriesThresholdCriteria criteria = gson.fromJson(gson.toJson(threshold), TimeSeriesThresholdCriteria.class);
        criteria.setThresholdType(type);
        thresholds.add(TimeSeriesThreshold.builder()
                           .accountId(getAccountId())
                           .projectIdentifier(getProjectIdentifier())
                           .dataSourceType(DataSourceType.STACKDRIVER)
                           .metricType(metricType)
                           .metricName(metricName)
                           .action(TimeSeriesThresholdActionType.IGNORE)
                           .criteria(criteria)
                           .build());
      });
    });
    return thresholds;
  }

  public void fromStackdriverDefinitions(
      List<StackdriverDefinition> stackdriverDefinitions, CVMonitoringCategory category) {
    Preconditions.checkNotNull(stackdriverDefinitions);
    if (metricInfoList == null) {
      metricInfoList = new ArrayList<>();
    }
    dashboardName = stackdriverDefinitions.get(0).getDashboardName();
    MetricPack metricPack = MetricPack.builder()
                                .category(category)
                                .accountId(getAccountId())
                                .dataSourceType(DataSourceType.STACKDRIVER)
                                .projectIdentifier(getProjectIdentifier())
                                .identifier(category.getDisplayName())
                                .build();

    stackdriverDefinitions.forEach(definition -> {
      TimeSeriesMetricType metricType = definition.getRiskProfile().getMetricType();
      metricInfoList.add(MetricInfo.builder()
                             .metricName(definition.getMetricName())
                             .jsonMetricDefinition(JsonUtils.asJson(definition.getJsonMetricDefinition()))
                             .metricType(metricType)
                             .tags(definition.getMetricTags())
                             .build());

      // add this metric to the pack and the corresponding thresholds
      Set<TimeSeriesThreshold> thresholds = getThresholdsToCreateOnSave(
          definition.getMetricName(), metricType, definition.getRiskProfile().getThresholdTypes());
      metricPack.addToMetrics(MetricDefinition.builder()
                                  .thresholds(new ArrayList<>(thresholds))
                                  .type(metricType)
                                  .name(definition.getMetricName())
                                  .included(true)
                                  .build());
    });
    this.setMetricPack(metricPack);
  }
}

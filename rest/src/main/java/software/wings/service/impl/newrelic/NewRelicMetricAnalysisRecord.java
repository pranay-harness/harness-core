package software.wings.service.impl.newrelic;

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
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.SortOrder;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "newRelicMetricAnalysisRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("workflowExecutionId"), @Field("stateExecutionId")
  }, options = @IndexOptions(unique = true, name = "analysisUniqueIdx"))
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class NewRelicMetricAnalysisRecord extends Base {
  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty private String message;

  @NotEmpty private RiskLevel riskLevel;

  @NotEmpty @Indexed private String applicationId;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  private List<NewRelicMetricAnalysis> metricAnalyses;

  private int analysisMinute;

  private boolean showTimeSeries;

  public void addNewRelicMetricAnalysis(NewRelicMetricAnalysis analysis) {
    metricAnalyses.add(analysis);
  }

  @Data
  @Builder
  public static class NewRelicMetricAnalysis implements Comparable<NewRelicMetricAnalysis> {
    private String metricName;
    private RiskLevel riskLevel;
    private List<NewRelicMetricAnalysisValue> metricValues;
    @Transient private String displayName;
    @Transient private String fullMetricName;

    public void addNewRelicMetricAnalysisValue(NewRelicMetricAnalysisValue metricAnalysisValue) {
      metricValues.add(metricAnalysisValue);
    }

    @Override
    public int compareTo(NewRelicMetricAnalysis other) {
      int riskDiff = this.riskLevel.compareTo(other.riskLevel);

      if (riskDiff != 0) {
        return riskDiff;
      }

      if (metricValues != null) {
        for (SortOrder sortOrder : NewRelicMetricValueDefinition.SORTING_METRIC_NAME.values()) {
          for (NewRelicMetricAnalysisValue metricAnalysisValue : metricValues) {
            if (metricAnalysisValue.getName().equals(sortOrder.getFieldName())) {
              if (other.metricValues != null) {
                for (NewRelicMetricAnalysisValue otherMetricAnalysisValue : other.metricValues) {
                  if (otherMetricAnalysisValue.getName().equals(sortOrder.getFieldName())) {
                    int sortCriteriaDiff = 0;
                    switch (sortOrder.getOrderType()) {
                      case ASC:
                        sortCriteriaDiff =
                            (int) (metricAnalysisValue.getTestValue() - otherMetricAnalysisValue.getTestValue());
                        break;
                      case DESC:
                        sortCriteriaDiff =
                            (int) (otherMetricAnalysisValue.getTestValue() - metricAnalysisValue.getTestValue());
                        break;
                      default:
                        throw new WingsException("Invalid sort order " + sortOrder.getOrderType());
                    }

                    if (sortCriteriaDiff != 0) {
                      return sortCriteriaDiff;
                    }
                  }
                }
              }
            }
          }
        }
      }

      return this.metricName.compareTo(other.metricName);
    }
  }

  @Data
  @Builder
  public static class NewRelicMetricAnalysisValue {
    private String name;
    private RiskLevel riskLevel;
    private double testValue;
    private double controlValue;
    private List<NewRelicMetricHostAnalysisValue> hostAnalysisValues;
  }

  @Data
  @Builder
  public static class NewRelicMetricHostAnalysisValue {
    private RiskLevel riskLevel;
    private String testHostName;
    private String controlHostName;
    private List<Double> testValues;
    private List<Double> controlValues;
  }
}

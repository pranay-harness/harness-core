package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.Constants.ML_RECORDS_TTL_MONTHS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.SortOrder;
import io.harness.exception.WingsException;
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
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

/**
 * Created by rsingh on 08/30/17.
 */
@Entity(value = "newRelicMetricAnalysisRecords", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("workflowExecutionId"), @Field("stateExecutionId"), @Field("groupName"), @Field("analysisMinute")
  }, options = @IndexOptions(unique = true, name = "analysisUniqueIdx"))
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicMetricAnalysisRecord extends Base implements Comparable<NewRelicMetricAnalysisRecord> {
  @NotEmpty private StateType stateType;

  @NotEmpty private String message;

  @NotEmpty private RiskLevel riskLevel;

  @NotEmpty private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  private String cvConfigId;

  private List<NewRelicMetricAnalysis> metricAnalyses;

  private int analysisMinute;

  private boolean showTimeSeries;

  private String baseLineExecutionId;

  @Default private String groupName = DEFAULT_GROUP_NAME;

  private String dependencyPath;

  private TimeSeriesMlAnalysisType mlAnalysisType;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());

  @Builder
  public NewRelicMetricAnalysisRecord(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, StateType stateType, String message,
      RiskLevel riskLevel, String workflowExecutionId, String stateExecutionId, String cvConfigId, String groupName,
      String dependencyPath, TimeSeriesMlAnalysisType mlAnalysisType, List<NewRelicMetricAnalysis> metricAnalyses,
      int analysisMinute, boolean showTimeSeries, String baseLineExecutionId) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.stateType = stateType;
    this.message = message;
    this.riskLevel = riskLevel;
    this.workflowExecutionId = workflowExecutionId;
    this.stateExecutionId = stateExecutionId;
    this.cvConfigId = cvConfigId;
    this.groupName = groupName;
    this.dependencyPath = dependencyPath;
    this.mlAnalysisType = mlAnalysisType;
    this.metricAnalyses = metricAnalyses;
    this.analysisMinute = analysisMinute;
    this.showTimeSeries = showTimeSeries;
    this.baseLineExecutionId = baseLineExecutionId;
    this.groupName = groupName;
    this.validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
  }

  public void addNewRelicMetricAnalysis(NewRelicMetricAnalysis analysis) {
    metricAnalyses.add(analysis);
  }

  @Override
  public int compareTo(NewRelicMetricAnalysisRecord o) {
    int analysisTypeComparison = this.mlAnalysisType.compareTo(o.mlAnalysisType);
    if (analysisTypeComparison != 0) {
      return analysisTypeComparison;
    }

    return this.groupName.compareTo(o.groupName);
  }

  @Data
  @Builder
  public static class NewRelicMetricAnalysis implements Comparable<NewRelicMetricAnalysis> {
    private String metricName;
    private RiskLevel riskLevel;
    private List<NewRelicMetricAnalysisValue> metricValues;
    @Transient private String displayName;
    @Transient private String fullMetricName;
    private String tag;

    public String getTag() {
      return isEmpty(tag) ? "DEFAULT" : tag;
    }

    public void addNewRelicMetricAnalysisValue(NewRelicMetricAnalysisValue metricAnalysisValue) {
      if (metricAnalysisValue.getTestValue() >= 0.0 || metricAnalysisValue.getControlValue() >= 0.0) {
        metricValues.add(metricAnalysisValue);
      }
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
    private String type;
    private String alertType;
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
    private List<Integer> anomalies;
    int testStartIndex;
  }
}

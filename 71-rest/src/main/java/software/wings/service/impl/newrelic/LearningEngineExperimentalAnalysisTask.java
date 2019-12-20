package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotation.IgnoreUnusedIndex;
import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.Base;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 1/8/18.
 */
@Indexes({
  @Index(fields =
      {
        @Field("state_execution_id")
        , @Field(value = "analysis_minute", type = IndexType.DESC), @Field("executionStatus"),
            @Field("ml_analysis_type"), @Field("cluster_level"), @Field("group_name"), @Field("version"),
            @Field(value = "createdAt", type = IndexType.DESC)
      },
      options = @IndexOptions(name = "taskQueueIdx"))
  ,
      @Index(fields = {
        @Field("cvConfigId"), @Field(value = "analysis_minute", type = IndexType.DESC), @Field("executionStatus")
      }, options = @IndexOptions(name = "cvConfigStatusIdx")), @Index(fields = {
        @Field("executionStatus")
        , @Field("ml_analysis_type"), @Field(value = "is24x7Task"), @Field(value = "createdAt", type = IndexType.DESC)
      }, options = @IndexOptions(name = "usageMetricsIndex")), @Index(fields = {
        @Field("experiment_name")
        , @Field("executionStatus"), @Field(value = "retry"), @Field(value = "createdAt", type = IndexType.DESC)
      }, options = @IndexOptions(name = "taskFetchIdx"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@FieldNameConstants(innerTypeName = "LearningEngineExperimentalAnalysisTaskKeys")
@IgnoreUnusedIndex
@Entity(value = "learningEngineExperimentalAnalysisTask", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class LearningEngineExperimentalAnalysisTask extends Base {
  public static long TIME_SERIES_ANALYSIS_TASK_TIME_OUT = TimeUnit.MINUTES.toMillis(2);
  public static final int RETRIES = 3;

  private String workflow_id;
  private String workflow_execution_id;
  @Indexed private String state_execution_id;
  private String service_id;
  private String auth_token;
  private int analysis_start_min;
  private long analysis_minute;
  private int smooth_window;
  private int tolerance;
  private int min_rpm;
  private int comparison_unit_window;
  private int parallel_processes;
  private String test_input_url;
  private String control_input_url;
  @Default private String group_name = NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
  private TimeSeriesMlAnalysisType time_series_ml_analysis_type;
  private String analysis_save_url;
  private String metric_template_url;
  private String log_analysis_get_url;
  private int analysis_start_time;
  private double sim_threshold;
  private Integer cluster_level;
  private List<String> query;
  private Set<String> control_nodes;
  private Set<String> test_nodes;
  private StateType stateType;
  private MLAnalysisType ml_analysis_type;
  private String experiment_name;
  private String feedback_url;
  private String feature_name;
  private String cvConfigId;
  private int prediction_start_time;
  private String previous_analysis_url;
  private String historical_analysis_url;
  private String previous_anomalies_url;
  private String cumulative_sums_url;
  private boolean is24x7Task;
  private String tag;
  private AnalysisComparisonStrategy analysis_comparison_strategy;
  @Indexed private ExecutionStatus executionStatus;
  private Double alertThreshold;
  @JsonProperty("log_ml_result_url") private String logMLResultUrl;
  @JsonProperty("use_supervised_model") private boolean shouldUseSupervisedModel;
  @JsonProperty("key_transactions_url") private String keyTransactionsUrl;

  @Builder.Default
  private ServiceApiVersion version = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];

  private int retry;

  @Default
  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusHours(8).toInstant());
}

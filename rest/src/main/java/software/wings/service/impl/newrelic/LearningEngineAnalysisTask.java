package software.wings.service.impl.newrelic;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 1/8/18.
 */
@Entity(value = "learningEngineAnalysisTask", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("workflow_execution_id")
    , @Field("state_execution_id"), @Field("executionStatus"), @Field("analysis_minute"), @Field("cluster_level"),
        @Field("ml_analysis_type"), @Field("control_nodes"), @Field("group_name")
  }, options = @IndexOptions(unique = true, name = "metricUniqueIdx"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class LearningEngineAnalysisTask extends Base {
  public static long TIME_SERIES_ANALYSIS_TASK_TIME_OUT = TimeUnit.MINUTES.toMillis(12);
  public static final int RETRIES = 3;

  private String workflow_id;
  private @Indexed String workflow_execution_id;
  private @Indexed String state_execution_id;
  private String service_id;
  private String auth_token;
  private int analysis_start_min;
  private @Indexed int analysis_minute;
  private int smooth_window;
  private int tolerance;
  private int min_rpm;
  private int comparison_unit_window;
  private int parallel_processes;
  private String test_input_url;
  private String control_input_url;
  private String analysis_save_url;
  private String metric_template_url;
  private String log_analysis_get_url;
  @Default private String group_name = NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
  private TimeSeriesMlAnalysisType time_series_ml_analysis_type;
  private int analysis_start_time;
  private double sim_threshold;
  private Integer cluster_level;
  private List<String> query;
  private Set<String> control_nodes;
  private Set<String> test_nodes;
  private StateType stateType;
  private MLAnalysisType ml_analysis_type;
  private String feedback_url;
  private String feature_name;
  private @Indexed ExecutionStatus executionStatus;

  @Builder.Default
  private ServiceApiVersion version = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];

  private int retry;
}

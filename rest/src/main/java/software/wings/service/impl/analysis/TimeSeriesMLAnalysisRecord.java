package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

/**
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Entity(value = "timeSeriesAnalysisRecords", noClassnameStored = true)
@Data
public class TimeSeriesMLAnalysisRecord extends Base {
  @NotEmpty @Indexed private String applicationId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private int analysisMinute;

  private Map<String, TimeSeriesMLTxnSummary> transactions;
}

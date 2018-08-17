package software.wings.service.impl.analysis;

import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.sm.StateType;

import java.util.Map;

/**
 * Common Class extended by TimeSeries and Experimental Analysis Record.
 * Created by Pranjal on 08/16/2018
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MetricAnalysisRecord extends Base {
  // Represents type of State
  @NotEmpty @Indexed private StateType stateType;

  // Work flow exec id
  @NotEmpty @Indexed private String workflowExecutionId;

  // State exec id
  @NotEmpty @Indexed private String stateExecutionId;

  // no. of minutes of analysis
  @NotEmpty @Indexed private int analysisMinute;

  @Indexed private String groupName = DEFAULT_GROUP_NAME;

  private String baseLineExecutionId;

  private Map<String, TimeSeriesMLTxnSummary> transactions;

  private String message;
}

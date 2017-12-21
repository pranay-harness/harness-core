package software.wings.service.impl.analysis;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.sm.StateType;

import java.util.Map;

/**
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Entity(value = "timeSeriesAnalysisRecords", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("workflowExecutionId")
                           , @Field("stateExecutionId"), @Field("analysisMinute") },
    options = @IndexOptions(unique = true, name = "MetricAnalysisUniqueIdx")))
@Data
public class TimeSeriesMLAnalysisRecord extends Base {
  @NotEmpty @Indexed private StateType stateType;

  @NotEmpty @Indexed private String applicationId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private int analysisMinute;

  private Map<String, TimeSeriesMLTxnSummary> transactions;
}

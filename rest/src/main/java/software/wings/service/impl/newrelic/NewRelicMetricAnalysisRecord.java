package software.wings.service.impl.newrelic;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.RiskLevel;

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
public class NewRelicMetricAnalysisRecord extends Base {
  @NotEmpty @Indexed private String message;

  @NotEmpty @Indexed private String workflowId;

  @NotEmpty @Indexed private String workflowExecutionId;

  @NotEmpty @Indexed private String stateExecutionId;

  private List<NewRelicMetricAnalysis> metricAnalyses;

  @Data
  private static class NewRelicMetricAnalysis {
    private String metricName;
    private RiskLevel riskLevel;
    private List<NewRelicMetricValue> metricValues;
  }

  @Data
  private static class NewRelicMetricValue {
    private String name;
    private RiskLevel riskLevel;
    private double testValue;
    private double controlValue;
  }
}

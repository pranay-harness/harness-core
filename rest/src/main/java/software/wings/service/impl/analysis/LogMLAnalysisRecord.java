package software.wings.service.impl.analysis;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/23/17.
 */
@Entity(value = "logAnalysisRecords", noClassnameStored = true)
@Data
public class LogMLAnalysisRecord extends Base {
  @NotEmpty @Indexed private String stateExecutionId;

  @NotEmpty @Indexed private String applicationId;

  @NotEmpty @Indexed private StateType stateType;

  private String query;
  private String analysisSummaryMessage;
  private List<List<SplunkAnalysisCluster>> unknown_events;
  private Map<String, List<SplunkAnalysisCluster>> test_events;
  private Map<String, List<SplunkAnalysisCluster>> control_events;
  private Map<String, Map<String, SplunkAnalysisCluster>> control_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> test_clusters;
}

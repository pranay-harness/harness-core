package software.wings.service.impl.splunk;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/23/17.
 */
@Entity(value = "splunkAnalysisRecords", noClassnameStored = true)
public class SplunkLogMLAnalysisRecord extends Base {
  @NotEmpty @Indexed private String stateExecutionInstanceId;

  @NotEmpty @Indexed private String applicationId;

  private List<List<SplunkAnalysisCluster>> unknown_events;
  private Map<String, List<SplunkAnalysisCluster>> test_events;
  private Map<String, List<SplunkAnalysisCluster>> control_events;
  private Map<String, Map<String, SplunkAnalysisCluster>> control_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters;
  private Map<String, Map<String, SplunkAnalysisCluster>> test_clusters;

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public List<List<SplunkAnalysisCluster>> getUnknown_events() {
    return unknown_events;
  }

  public void setUnknown_events(List<List<SplunkAnalysisCluster>> unknown_events) {
    this.unknown_events = unknown_events;
  }

  public Map<String, List<SplunkAnalysisCluster>> getTest_events() {
    return test_events;
  }

  public void setTest_events(Map<String, List<SplunkAnalysisCluster>> test_events) {
    this.test_events = test_events;
  }

  public Map<String, List<SplunkAnalysisCluster>> getControl_events() {
    return control_events;
  }

  public void setControl_events(Map<String, List<SplunkAnalysisCluster>> control_events) {
    this.control_events = control_events;
  }

  public Map<String, Map<String, SplunkAnalysisCluster>> getControl_clusters() {
    return control_clusters;
  }

  public void setControl_clusters(Map<String, Map<String, SplunkAnalysisCluster>> control_clusters) {
    this.control_clusters = control_clusters;
  }

  public Map<String, Map<String, SplunkAnalysisCluster>> getUnknown_clusters() {
    return unknown_clusters;
  }

  public void setUnknown_clusters(Map<String, Map<String, SplunkAnalysisCluster>> unknown_clusters) {
    this.unknown_clusters = unknown_clusters;
  }

  public Map<String, Map<String, SplunkAnalysisCluster>> getTest_clusters() {
    return test_clusters;
  }

  public void setTest_clusters(Map<String, Map<String, SplunkAnalysisCluster>> test_clusters) {
    this.test_clusters = test_clusters;
  }
}

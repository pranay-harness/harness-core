package software.wings.service.impl.splunk;

import java.util.List;

/**
 * Created by rsingh on 6/23/17.
 */
public class SplunkMLAnalysisResponse {
  private List<String> args;
  private String appId;
  private String stateExecutionInstanceId;
  private String splunkMlVersion;

  private List<SplunkLogAnalysisRecord> events;

  public List<String> getArgs() {
    return args;
  }

  public void setArgs(List<String> args) {
    this.args = args;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public String getSplunkMlVersion() {
    return splunkMlVersion;
  }

  public void setSplunkMlVersion(String splunkMlVersion) {
    this.splunkMlVersion = splunkMlVersion;
  }

  public List<SplunkLogAnalysisRecord> getEvents() {
    return events;
  }

  public void setEvents(List<SplunkLogAnalysisRecord> events) {
    this.events = events;
  }

  public List<SplunkLogAnalysisRecord> generateRecords() {
    for (SplunkLogAnalysisRecord logAnalysisRecord : getEvents()) {
      logAnalysisRecord.setAppId(getAppId());
      logAnalysisRecord.setStateExecutionInstanceId(getStateExecutionInstanceId());
      logAnalysisRecord.setSplunkMlVersion(getSplunkMlVersion());
    }

    return getEvents();
  }
}

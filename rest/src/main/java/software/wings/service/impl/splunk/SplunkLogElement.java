package software.wings.service.impl.splunk;

/**
 * Created by rsingh on 6/20/17.
 */
public class SplunkLogElement {
  private String clusterLabel;
  private String host;
  private long timeStamp;
  private int count;
  private String logMessage;

  public String getClusterLabel() {
    return clusterLabel;
  }

  public void setClusterLabel(String clusterLabel) {
    this.clusterLabel = clusterLabel;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }
}

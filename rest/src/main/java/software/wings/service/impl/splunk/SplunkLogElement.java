package software.wings.service.impl.splunk;

import lombok.Data;

/**
 * Created by rsingh on 6/20/17.
 */
@Data
public class SplunkLogElement {
  private String clusterLabel;
  private String host;
  private long timeStamp;
  private int count;
  private String logMessage;
  private int logCollectionMinute;
}

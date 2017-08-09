package software.wings.service.impl.analysis;

import lombok.Data;

/**
 * Created by rsingh on 6/20/17.
 */
@Data
public class LogElement {
  private String query;
  private String clusterLabel;
  private String host;
  private long timeStamp;
  private int count;
  private String logMessage;
  private int logCollectionMinute;
}

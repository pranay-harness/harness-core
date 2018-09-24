package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 6/20/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogElement {
  private String query;
  private String clusterLabel;
  private String host;
  private long timeStamp;
  private int count;
  private String logMessage;
  private int logCollectionMinute;
}

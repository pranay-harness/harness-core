package software.wings.service.impl.splunk;

import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class SplunkMLClusterSummary {
  private String host;
  private String logText;
  private int count;
  private List<String> tags;
  private double xCordinate;
  private double yCordinate;
  private boolean unexpectedFreq;
}

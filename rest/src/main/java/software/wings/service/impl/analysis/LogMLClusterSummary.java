package software.wings.service.impl.analysis;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class LogMLClusterSummary {
  private Map<String, LogMLHostSummary> hostSummary;
  private String logText;
  private List<String> tags;
}

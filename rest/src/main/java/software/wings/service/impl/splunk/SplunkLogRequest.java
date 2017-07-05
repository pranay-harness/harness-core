package software.wings.service.impl.splunk;

import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
public class SplunkLogRequest {
  private final String applicationId;
  private final String stateExecutionId;
  private final List<String> nodes;
  private final int logCollectionMinute;
}

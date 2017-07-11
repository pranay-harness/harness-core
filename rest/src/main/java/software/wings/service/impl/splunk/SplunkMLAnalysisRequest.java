package software.wings.service.impl.splunk;

import lombok.Data;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
public class SplunkMLAnalysisRequest {
  private final String query;
  private final String applicationId;
  private final String stateExecutionId;
}

package software.wings.service.impl.analysis;

import lombok.Data;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
public class LogMLAnalysisRequest {
  private final String query;
  private final String applicationId;
  private final String stateExecutionId;
  private final Integer logCollectionMinute;
}

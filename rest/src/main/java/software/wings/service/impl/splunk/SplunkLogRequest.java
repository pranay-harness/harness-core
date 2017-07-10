package software.wings.service.impl.splunk;

import lombok.Data;
import software.wings.utils.JsonUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
public class SplunkLogRequest {
  private final String query;
  private final String applicationId;
  private final String stateExecutionId;
  private final List<String> nodes;
  private final int logCollectionMinute;
}

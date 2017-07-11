package software.wings.service.impl.appdynamics;

import lombok.Data;

/**
 * Created by rsingh on 6/21/17.
 */
@Data
public class AppdynamicsDataRequest {
  private final String applicationId;
  private final String stateExecutionId;
}

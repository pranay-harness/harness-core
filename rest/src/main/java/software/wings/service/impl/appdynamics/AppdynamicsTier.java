package software.wings.service.impl.appdynamics;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 5/12/17.
 */
@Data
@Builder
public class AppdynamicsTier {
  private long id;
  private String name;
  private String description;
  private String type;
  private String agentType;
  private int numberOfNodes;
}

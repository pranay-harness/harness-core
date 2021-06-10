package software.wings.service.impl.appdynamics;

import lombok.Data;

/**
 * Created by rsingh on 5/11/17.
 */
@Data
public class AppdynamicsBusinessTransaction {
  private long id;
  private String name;
  private String entryPointType;
  private String internalName;
  private long tierId;
  private String tierName;
  private boolean background;
}

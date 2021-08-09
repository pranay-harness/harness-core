package software.wings.api;

import lombok.Builder;
import lombok.Data;

/**
 * Created by bzane on 9/11/17.
 */
@Data
@Builder
public class ContainerServiceData {
  private String name;
  private String image;
  // Use this if name can not be unique, like in case of ECS daemonSet
  private String uniqueIdentifier;
  private int previousCount;
  private int desiredCount;
  private int previousTraffic;
  private int desiredTraffic;
}

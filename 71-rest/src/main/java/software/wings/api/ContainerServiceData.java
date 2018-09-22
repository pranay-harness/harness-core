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
  private int previousCount;
  private int desiredCount;
  private int previousTraffic;
  private int desiredTraffic;
}

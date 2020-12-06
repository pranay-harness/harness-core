package software.wings.sm.states.pcf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteUpdateConfig {
  private String route;
  private String application;
  private boolean mapRoute;
}

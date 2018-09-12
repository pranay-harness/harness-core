package software.wings.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfraMappingElement {
  private Pcf pcf;

  @Builder
  @Data
  public static class Pcf {
    private String route;
    private String tempRoute;
  }
}

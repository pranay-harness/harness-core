package software.wings.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfraMappingElement {
  private Pcf pcf;
  private Kubernetes kubernetes;
  private Helm helm;

  @Builder
  @Data
  public static class Pcf {
    private String route;
    private String tempRoute;
  }

  @Builder
  @Data
  public static class Kubernetes {
    private String namespace;
  }

  @Builder
  @Data
  public static class Helm {
    private String shortId;
    private String releaseName;
  }
}

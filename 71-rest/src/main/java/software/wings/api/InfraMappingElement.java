package software.wings.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfraMappingElement {
  private Pcf pcf;
  private Kubernetes kubernetes;
  private Helm helm;
  private String name;
  private String infraId;

  @Data
  @Builder
  public static class Pcf {
    private String route;
    private String tempRoute;
    private CloudProvider cloudProvider;
    private String organization;
    private String space;
  }

  @Data
  @Builder
  public static class Kubernetes {
    private String namespace;
    private String infraId;
  }

  @Data
  @Builder
  public static class Helm {
    private String shortId;
    private String releaseName;
  }

  @Data
  @Builder
  public static class CloudProvider {
    private String name;
  }
}

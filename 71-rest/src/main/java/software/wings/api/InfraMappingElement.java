package software.wings.api;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class InfraMappingElement {
  private Pcf pcf;
  private Kubernetes kubernetes;
  private Helm helm;
  private Custom custom;
  private String name;
  private String infraId;
  private CloudProvider cloudProvider;

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

  @Data
  @Builder
  public static class Custom {
    private final Map<String, String> vars;
  }
}

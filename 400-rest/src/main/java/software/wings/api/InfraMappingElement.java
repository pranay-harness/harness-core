/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

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

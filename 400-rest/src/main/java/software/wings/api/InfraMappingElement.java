/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

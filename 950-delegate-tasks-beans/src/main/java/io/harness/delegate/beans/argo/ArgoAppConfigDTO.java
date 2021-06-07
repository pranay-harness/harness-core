package io.harness.delegate.beans.argo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgoAppConfigDTO {
  String appName;
  String project;
  String gitRepoUrl;
  String manifestPath;
  Sync sync;
  Cluster cluster;

  @Data
  @Builder
  public static class Sync {
    boolean validate;
    boolean ApplyOutOfSyncOnly;
  }

  @Data
  @Builder
  public static class Cluster {
    String url;
    String namespace;
  }
}

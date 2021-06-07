package io.harness.argo.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgoAppRequest {
  String name;
  String project;
  Destination destination;
  Source source;
  SyncPolicyOptions syncPolicyOptions;

  @Data
  @Builder
  public static class Destination {
    String namespace;
    String server;
  }

  @Data
  @Builder
  public static class Source {
    String path;
    String repoUrl;
    @Builder.Default String targetRevision = "HEAD";
  }

  @Data
  @Builder
  public static class SyncPolicyOptions {
    boolean validate;
    boolean ApplyOutOfSyncOnly;
  }
}

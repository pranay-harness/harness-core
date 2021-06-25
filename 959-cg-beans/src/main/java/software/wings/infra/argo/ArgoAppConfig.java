package software.wings.infra.argo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArgoAppConfig {
  String appName;
  String project;
  String gitRepoUrl;
  String manifestPath;
  String gitRef;
  Sync sync;
  String clusterUrl;

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Sync {
    boolean validate;
    boolean applyOutOfSyncOnly;
    boolean autoCreateNamespace;
    boolean replace;
    boolean pruneLast;
  }
}

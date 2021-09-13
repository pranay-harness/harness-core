package io.harness.cvng.beans.change;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesChangeEventMetadata extends ChangeEventMetadata {
  String oldYaml;
  String newYaml;
  Instant timestamp;
  String workload;
  String namespace;
  String kind;
  KubernetesResourceType resourceType;
  Action action;
  String reason;
  String message;

  public enum Action { Add, Update, Delete }
  public enum KubernetesResourceType { Deployment, ReplicaSet, Secret, Pod }

  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.KUBERNETES;
  }
}

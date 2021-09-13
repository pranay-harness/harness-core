package io.harness.cvng.beans.change;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
public class KubernetesChangeEventDTO extends ChangeEventDTO {
  KubernetesResourceType resourceType;
  Action action;
  String oldYaml;
  String newYaml;

  String namespace;
  String workloadName;
  String reason;
  String message;
  String kind;

  public enum Action { Add, Update, Delete }
  public enum KubernetesResourceType { Deployment, ReplicaSet, Secret, Pod }
}

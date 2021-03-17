package io.harness.cvng.activity.beans;

import io.harness.cvng.beans.activity.KubernetesActivityDTO.KubernetesEventType;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class KubernetesActivityDetailsDTO {
  String sourceName;
  String connectorIdentifier;
  String workload;
  String kind;
  String namespace;
  @Singular List<KubernetesActivityDetail> details;

  @Value
  @Builder
  public static class KubernetesActivityDetail {
    long timeStamp;
    KubernetesEventType eventType;
    String reason;
    String message;
    String eventJson;
  }
}

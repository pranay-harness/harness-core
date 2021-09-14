/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

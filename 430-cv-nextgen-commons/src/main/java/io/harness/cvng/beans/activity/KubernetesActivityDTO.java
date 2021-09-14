/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.activity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KUBERNETES")
public class KubernetesActivityDTO extends ActivityDTO {
  String namespace;
  String workloadName;
  String reason;
  String message;
  String kind;
  String activitySourceConfigId;
  String eventJson;
  KubernetesEventType eventType;

  @Override
  public ActivityType getType() {
    return ActivityType.KUBERNETES;
  }

  public enum KubernetesEventType { Normal, Warning, Error }
}

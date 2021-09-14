/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResource;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class NodeMetrics extends CustomResource {
  @SerializedName("timestamp") private String timestamp;
  @SerializedName("window") private String window;
  @SerializedName("usage") private Usage usage;

  @Builder
  public NodeMetrics(String name, String timestamp, String window, Usage usage) {
    this.getMetadata().setName(name);
    this.timestamp = timestamp;
    this.window = window;
    this.usage = usage;
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import static io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient.METRICS_API_GROUP;
import static io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient.METRICS_API_VERSION;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResource;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class PodMetrics extends CustomResource {
  @SerializedName("timestamp") private String timestamp;
  @SerializedName("window") private String window;
  @SerializedName("containers") private List<Container> containers;

  @Builder
  public PodMetrics(
      String name, String namespace, String timestamp, String window, @Singular List<Container> containers) {
    this.getMetadata().setName(name);
    this.getMetadata().setNamespace(namespace);
    this.setApiVersion(METRICS_API_GROUP + "/" + METRICS_API_VERSION);
    this.timestamp = timestamp;
    this.window = window;
    this.containers = containers;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @EqualsAndHashCode
  public static class Container implements Serializable {
    @SerializedName("name") String name;
    @SerializedName("usage") Usage usage;
  }
}

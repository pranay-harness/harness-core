/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.metrics.client.model;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class Usage implements Serializable {
  @SerializedName("cpu") String cpu;
  @SerializedName("memory") String memory;
}

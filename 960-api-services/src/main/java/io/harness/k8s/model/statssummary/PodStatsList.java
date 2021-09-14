/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model.statssummary;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.common.KubernetesType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@OwnedBy(CE)
public class PodStatsList implements KubernetesType {
  @SerializedName("kind") private String kind;
  @SerializedName("apiVersion") private String apiVersion;

  @SerializedName("pods")
  private List<PodStats> items =
      new ArrayList(); // initialized because better to have array of size 0 then having a null
}

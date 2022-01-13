/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

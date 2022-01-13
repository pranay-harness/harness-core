/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.beans.recommendation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterRecommendationAccuracy {
  @SerializedName("cpu") private Double cpu;

  @SerializedName("masterPrice") private Double masterPrice;

  @SerializedName("memory") private Double memory;

  @SerializedName("nodes") private Long nodes;

  @SerializedName("regularNodes") private Long regularNodes;

  @SerializedName("regularPrice") private Double regularPrice;

  @SerializedName("spotNodes") private Long spotNodes;

  @SerializedName("spotPrice") private Double spotPrice;

  @SerializedName("totalPrice") private Double totalPrice;

  @SerializedName("workerPrice") private Double workerPrice;

  @SerializedName("zone") private String zone;
}

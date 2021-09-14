/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

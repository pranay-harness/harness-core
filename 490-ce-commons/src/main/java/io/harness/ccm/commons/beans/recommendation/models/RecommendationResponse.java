/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.beans.recommendation.models;

import io.harness.ccm.commons.beans.billing.InstanceCategory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationResponse {
  @SerializedName("accuracy") private ClusterRecommendationAccuracy accuracy;

  @SerializedName("nodePools") private List<NodePool> nodePools;

  @SerializedName("provider") private String provider;

  @SerializedName("region") private String region;

  @SerializedName("service") private String service;

  @SerializedName("zone") private String zone;

  @Builder.Default private InstanceCategory instanceCategory = InstanceCategory.ON_DEMAND;
}

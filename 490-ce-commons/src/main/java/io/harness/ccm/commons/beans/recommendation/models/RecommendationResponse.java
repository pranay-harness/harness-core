/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

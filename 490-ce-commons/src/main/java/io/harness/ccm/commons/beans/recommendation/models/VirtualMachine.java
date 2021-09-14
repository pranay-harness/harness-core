/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.beans.recommendation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualMachine {
  @SerializedName("avgPrice") private Double avgPrice;

  @SerializedName("burst") private Boolean burst;

  @SerializedName("category") private String category;

  @SerializedName("cpusPerVm") private Double cpusPerVm;

  @SerializedName("currentGen") private Boolean currentGen;

  @SerializedName("gpusPerVm") private Double gpusPerVm;

  @SerializedName("memPerVm") private Double memPerVm;

  @SerializedName("allocatableCpusPerVm") private Double allocatableCpusPerVm;

  @SerializedName("allocatableMemPerVm") private Double allocatableMemPerVm;

  @SerializedName("networkPerf") private String networkPerf;

  @SerializedName("networkPerfCategory") private String networkPerfCategory;

  @SerializedName("onDemandPrice") private Double onDemandPrice;

  @SerializedName("type") private String type;

  @SerializedName("zones") private List<String> zones;
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pricing.dto.cloudinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDetails {
  @SerializedName("attributes") Map<String, String> attributes;

  @SerializedName("burst") Boolean burst;

  @SerializedName("category") String category;

  @SerializedName("cpusPerVm") Double cpusPerVm;

  @SerializedName("currentGen") Boolean currentGen;

  @SerializedName("gpusPerVm") Double gpusPerVm;

  @SerializedName("memPerVm") Double memPerVm;

  @SerializedName("ntwPerf") String ntwPerf;

  @SerializedName("ntwPerfCategory") String ntwPerfCategory;

  @SerializedName("onDemandPrice") Double onDemandPrice;

  @SerializedName("spotPrice") List<ZonePrice> spotPrice;

  @SerializedName("type") String type;

  @SerializedName("zones") List<String> zones;

  // Not present in cloudInfo response
  Double networkPrice;
}

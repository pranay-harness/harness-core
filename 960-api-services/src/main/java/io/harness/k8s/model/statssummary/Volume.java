/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model.statssummary;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Volume {
  @SerializedName("name") private String name;
  @SerializedName("availableBytes") private String availableBytes;
  @SerializedName("capacityBytes") private String capacityBytes;
  @SerializedName("usedBytes") private String usedBytes;
  @SerializedName("pvcRef") private PVCRef pvcRef;
  @SerializedName("time") private String time;

  /*
   *** Not required as of now
   * "inodesFree": 4913874,
   * "inodes": 4915200,
   * "inodesUsed": 1326
   */
}

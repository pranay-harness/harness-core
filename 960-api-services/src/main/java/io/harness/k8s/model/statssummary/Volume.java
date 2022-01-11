/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

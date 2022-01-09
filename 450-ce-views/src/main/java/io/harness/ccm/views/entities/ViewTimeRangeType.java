/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(
    description =
        "The type of Perspective time range filter, select CUSTOM is you want a Perspective between a fixed set of startTime and endTime")
public enum ViewTimeRangeType {
  LAST_7("last7"),
  LAST_30("last30"),
  LAST_MONTH("lastMonth"),
  CURRENT_MONTH("currentMonth"),
  CUSTOM("custom");

  @Getter private final String name;

  ViewTimeRangeType(String name) {
    this.name = name;
  }
}

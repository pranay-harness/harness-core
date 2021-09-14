/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.entities;

import lombok.Getter;

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

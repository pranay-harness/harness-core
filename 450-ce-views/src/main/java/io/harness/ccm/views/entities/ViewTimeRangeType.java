package io.harness.ccm.views.entities;

import lombok.Getter;

public enum ViewTimeRangeType {
  CUSTOM("custom"),
  LAST_7("last7"),
  LAST_30("last30"),
  LAST_MONTH("lastMonth");

  @Getter private final String name;

  ViewTimeRangeType(String name) {
    this.name = name;
  }
}

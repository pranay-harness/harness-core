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

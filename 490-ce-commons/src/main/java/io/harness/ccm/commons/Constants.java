package io.harness.ccm.commons;

import java.time.ZoneOffset;

public interface Constants {
  /**
   * The constant HARNESS_NAME.
   */
  String HARNESS_NAME = "Harness";

  /**
   * Offset used while saving into timescaleDB
   */
  ZoneOffset ZONE_OFFSET = ZoneOffset.UTC;
}

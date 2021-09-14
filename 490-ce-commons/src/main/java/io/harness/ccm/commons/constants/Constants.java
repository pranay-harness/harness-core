/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.constants;

import java.time.ZoneOffset;
import java.util.TimeZone;

public interface Constants {
  /**
   * The constant HARNESS_NAME.
   */
  String HARNESS_NAME = "Harness";

  /**
   * Offset used while saving into timescaleDB
   */
  ZoneOffset ZONE_OFFSET = ZoneOffset.UTC;

  TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
}

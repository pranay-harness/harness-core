package io.harness.ccm.commons.constants;

import io.harness.grpc.IdentifierKeys;

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

  String CLUSTER_ID_IDENTIFIER = IdentifierKeys.PREFIX + "clusterId";
  String UID = IdentifierKeys.PREFIX + "uid";
}

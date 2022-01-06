/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DataFormatException;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.experimental.UtilityClass;

/**
 * Utility class to handle mapping between {@link Timestamp} and java timestamp types.
 */
@OwnedBy(HarnessTeam.CE)
@UtilityClass
@ParametersAreNonnullByDefault
public class HTimestamps {
  public Timestamp fromInstant(Instant instant) {
    return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
  }

  public Instant toInstant(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  public Timestamp fromMillis(long epoch) {
    return fromInstant(Instant.ofEpochMilli(epoch));
  }

  public long toMillis(Timestamp timestamp) {
    return toInstant(timestamp).toEpochMilli();
  }

  public Timestamp fromDate(Date date) {
    return fromInstant(date.toInstant());
  }

  public Date toDate(Timestamp timestamp) {
    return Date.from(toInstant(timestamp));
  }

  public Timestamp parse(String time) {
    try {
      return Timestamps.parse(time);
    } catch (ParseException e) {
      throw new DataFormatException("Unparseable timestamp", e);
    }
  }
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.utils;

import static io.harness.ccm.commons.utils.TimeUtils.toOffsetDateTime;

import static org.jooq.impl.DSL.val;

import io.harness.timescaledb.Routines;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.RowCountQuery;

@UtilityClass
@Slf4j
public class TimescaleUtils {
  public int execute(@NonNull RowCountQuery finalStep) {
    return retryRun(finalStep::execute);
  }

  // TODO(UTSAV): Restrict the default exceptions to java.net.SocketException in future after proper testing.
  @SneakyThrows
  public <E> E retryRun(@NonNull Callable<E> callable) {
    return retryRun(callable, 3, ImmutableSet.of(Exception.class));
  }

  @SneakyThrows
  public <E> E retryRun(
      @NonNull Callable<E> callable, int retryCount, @NonNull final Set<Class<? extends Exception>> exceptions) {
    for (int i = 1; i <= retryCount; i++) {
      try {
        return callable.call();
      } catch (Exception ex) {
        if (!shouldRetryOn(ex, exceptions)) {
          log.info("Caught {} and its not set to retry on", ex.getClass());
          throw ex;
        }
        log.warn("Failed to execute, attempt {}, caused by ", i, ex);

        if (retryCount == i) {
          log.error("Retry exhausted, couldnt execute");
          throw ex;
        }

        Thread.sleep(100L);
      }
    }

    throw new Exception("Unreachable statement, unknown error occurred");
  }

  private static boolean shouldRetryOn(Exception ex, @NonNull final Set<Class<? extends Exception>> exceptions) {
    return exceptions.stream().anyMatch(c -> c.isAssignableFrom(ex.getClass()));
  }

  public static Condition isAlive(
      Field<OffsetDateTime> STARTTIME, Field<OffsetDateTime> STOPTIME, long jobStartTime, long jobEndTime) {
    return Routines.isAlive(STARTTIME, STOPTIME, val(toOffsetDateTime(jobStartTime)), val(toOffsetDateTime(jobEndTime)))
        .eq(true);
  }

  public static Condition isAliveAtInstant(@NonNull Field<OffsetDateTime> startTimeField,
      @NonNull Field<OffsetDateTime> stopTimeField, @NonNull Instant atInstant) {
    return startTimeField.le(toOffsetDateTime(atInstant))
        .and(stopTimeField.isNull().or(stopTimeField.ge(toOffsetDateTime(atInstant))));
  }
}

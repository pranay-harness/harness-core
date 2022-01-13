/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.timescaledb.metrics;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;

@Slf4j
@Singleton
public class HExecuteListener extends DefaultExecuteListener implements QueryStatsPrinter {
  private static final long toSeconds = 1000;
  private static long slowQuery = 1 * toSeconds;
  private static long extremelySlowQuery = 2 * toSeconds;
  private static final String START_TIME = "startTime";

  private final Map<String, QueryStat> queryStatMap = new ConcurrentHashMap<>();

  private static HExecuteListener instance;

  /**
   * Slowness is subjective to where this metrics is used, e.g., batch-processing query may have higher execution time
   */
  public static HExecuteListener getInstance(long slowQuerySeconds, long extremelySlowQuerySeconds) {
    if (instance == null) {
      instance = new HExecuteListener(slowQuerySeconds, extremelySlowQuerySeconds);
    }
    return instance;
  }

  public static HExecuteListener getInstance() {
    if (instance == null) {
      instance = new HExecuteListener();
    }
    return instance;
  }

  private HExecuteListener() {}

  private HExecuteListener(long slowQuerySeconds, long extremelySlowQuerySeconds) {
    slowQuery = slowQuerySeconds * toSeconds;
    extremelySlowQuery = extremelySlowQuerySeconds * toSeconds;
  }

  @Override
  public void executeStart(ExecuteContext ctx) {
    ctx.data(START_TIME, Instant.now().toEpochMilli());
  }

  @Override
  public void executeEnd(ExecuteContext ctx) {
    try {
      long endTime = Instant.now().toEpochMilli();
      String sqlQuery = ctx.sql().replace("\"", "").replace("public.", "");

      long startTime = (long) ctx.data(START_TIME);
      long diff = endTime - startTime;

      QueryStat queryStat =
          queryStatMap.computeIfAbsent(sqlQuery, k -> new QueryStat()).update(diff / (double) toSeconds);

      if (diff >= extremelySlowQuery) {
        log.warn("extremelySlowQuery !!!: Took {} secs to execute {}\n{}", diff / (double) toSeconds, ctx.query(),
            queryStat);
      } else if (diff >= slowQuery) {
        log.info("slowQuery !: Took {} secs to execute {}\n{}", diff / (double) toSeconds, ctx.query(), queryStat);
      }

    } catch (Exception ex) {
      log.warn("This is good :), a statsListener shouldnt interfere with query execution", ex);
    }
  }

  @Override
  public Map<String, QueryStat> get() {
    return ImmutableMap.copyOf(queryStatMap);
  }
}

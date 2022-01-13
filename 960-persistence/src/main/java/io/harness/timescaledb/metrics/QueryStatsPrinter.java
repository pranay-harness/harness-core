/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.timescaledb.metrics;

import java.util.Map;
import lombok.ToString;

public interface QueryStatsPrinter {
  Map<String, QueryStat> get();

  @ToString
  class QueryStat {
    public int count;
    public double avgExecutionTime;

    public double maxExecutionTime;
    public double secondMaxExecutionTime;

    public QueryStat update(double val) {
      updateMaxStats(val);

      avgExecutionTime = (avgExecutionTime * count + val) / (count + 1);

      count++;
      return this;
    }

    private void updateMaxStats(double val) {
      if (val >= maxExecutionTime) {
        secondMaxExecutionTime = maxExecutionTime;
        maxExecutionTime = val;
      } else if (val > secondMaxExecutionTime) {
        secondMaxExecutionTime = val;
      }
    }
  }
}

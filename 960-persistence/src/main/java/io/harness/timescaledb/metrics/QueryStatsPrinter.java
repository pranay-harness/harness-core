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

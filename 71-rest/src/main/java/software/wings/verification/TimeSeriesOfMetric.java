package software.wings.verification;

import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vaibhav Tulsyan
 * 24/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSeriesOfMetric implements Comparable<TimeSeriesOfMetric> {
  private static final Logger logger = LoggerFactory.getLogger(TimeSeriesOfMetric.class);

  @Builder.Default int risk = -1;
  private String metricName;
  private String metricType;
  private String metricDeeplinkUrl;
  private boolean isLongTermPattern;
  private long lastSeenTime;

  private SortedMap<Long, TimeSeriesDataPoint> timeSeries;
  private SortedMap<Long, TimeSeriesRisk> risksForTimeSeries;

  public Collection<TimeSeriesDataPoint> getTimeSeries() {
    return timeSeries.values();
  }

  @JsonIgnore
  public Map<Long, TimeSeriesDataPoint> getTimeSeriesMap() {
    return timeSeries;
  }

  public Collection<TimeSeriesRisk> getRisksForTimeSeries() {
    if (risksForTimeSeries != null) {
      return risksForTimeSeries.values();
    }
    return new ArrayList<>();
  }

  @JsonIgnore
  public Map<Long, TimeSeriesRisk> getTimeSeriesRiskMap() {
    return risksForTimeSeries;
  }

  public void updateRisk(int newRisk) {
    if (newRisk > risk) {
      risk = newRisk;
    }
  }

  public void addToRiskMap(long analysisTime, int risk) {
    if (risksForTimeSeries == null) {
      risksForTimeSeries = new TreeMap<>();
    }
    risksForTimeSeries.put(analysisTime,
        TimeSeriesRisk.builder()
            .startTime(analysisTime - TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES) + 1)
            .endTime(analysisTime)
            .risk(risk)
            .build());
  }

  public void addToTimeSeriesMap(long dataCollectionMinute, double metricValue) {
    long dataCollectionMillis = TimeUnit.MINUTES.toMillis(dataCollectionMinute);
    if (!timeSeries.containsKey(dataCollectionMillis)) {
      logger.error("Incorrect time " + dataCollectionMinute + " in TimeSeries API for metric: " + metricName);
      return;
    }
    timeSeries.put(
        dataCollectionMillis, TimeSeriesDataPoint.builder().timestamp(dataCollectionMillis).value(metricValue).build());
  }

  public void setLongTermPattern(int longTermPattern) {
    isLongTermPattern = longTermPattern == 1;
  }

  @Override
  public int compareTo(@NotNull TimeSeriesOfMetric o) {
    if (o.risk != this.risk) {
      return o.risk - this.risk;
    }

    if (this.timeSeries != null && o.timeSeries != null) {
      AtomicInteger numEmptyThis = new AtomicInteger(0);
      AtomicInteger numEmptyOther = new AtomicInteger(0);
      this.timeSeries.forEach((k, v) -> {
        if (v.getValue() == -1) {
          numEmptyThis.getAndIncrement();
        }
      });

      o.timeSeries.forEach((k, v) -> {
        if (v.getValue() == -1) {
          numEmptyOther.getAndIncrement();
        }
      });
      if (numEmptyThis.get() != numEmptyOther.get()) {
        return numEmptyThis.get() - numEmptyOther.get();
      }
    }

    return this.metricName.compareTo(o.metricName);
  }
}

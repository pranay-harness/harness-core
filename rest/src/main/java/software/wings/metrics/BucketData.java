package software.wings.metrics;

import com.google.common.math.Stats;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by mike@ on 4/11/17.
 */
public class BucketData {
  private long startTimeMillis;
  private long endTimeMillis;
  private RiskLevel risk;
  private Stats stats;
  private String displayValue;

  public BucketData(long startTimeMillis, long endTimeMillis, RiskLevel risk, Stats stats, String displayValue) {
    this.startTimeMillis = startTimeMillis;
    this.endTimeMillis = endTimeMillis;
    this.risk = risk;
    this.stats = stats;
    this.displayValue = displayValue;
  }

  public long getStartTimeMillis() {
    return startTimeMillis;
  }

  public void setStartTimeMillis(long startTimeMillis) {
    this.startTimeMillis = startTimeMillis;
  }

  public long getEndTimeMillis() {
    return endTimeMillis;
  }

  public void setEndTimeMillis(long endTimeMillis) {
    this.endTimeMillis = endTimeMillis;
  }

  public RiskLevel getRisk() {
    return risk;
  }

  public void setRisk(RiskLevel risk) {
    this.risk = risk;
  }

  public Stats getStats() {
    return stats;
  }

  public void setStats(Stats stats) {
    this.stats = stats;
  }

  public String getDisplayValue() {
    return displayValue;
  }

  public void setDisplayValue(String displayValue) {
    this.displayValue = displayValue;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("Start: ").append(startTimeMillis).append(" (").append(new Date(startTimeMillis).toString()).append(")\n");
    s.append("End: ").append(endTimeMillis).append(" (").append(new Date(endTimeMillis).toString()).append(")\n");
    s.append("Risk: ").append(risk.name()).append("\n");
    s.append("DisplayValue: ").append(displayValue).append("\n");
    s.append("Stats: ").append(stats);
    return s.toString();
  }
}

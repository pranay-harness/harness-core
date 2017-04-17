package software.wings.metrics;

import com.google.common.math.DoubleMath;
import com.google.common.math.Stats;

import com.github.reinert.jjschema.Attributes;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Metrics that reflect the time taken to do something.
 * Created by mike@ on 4/7/17.
 */
public class PercentageMetric<T extends Number> extends Metric<T> {
  @Attributes(required = true, title = "Threshold", description = "3.0") private double threshold;
  // if alertWhenMoreThan is true, the bad state is when value is above threshold
  // conversely, if alertWhenMoreThan is false, the bad state is when value is below threshold
  @Attributes(required = true, title = "Alert When More Than?", description = "True") private boolean alertWhenMoreThan;

  public PercentageMetric(String name, String path, MetricType type, double threshold, boolean alertWhenMoreThan) {
    super(name, path, type, true);
    this.threshold = threshold;
    this.alertWhenMoreThan = alertWhenMoreThan;
  }

  @Override
  public RiskLevel generateRiskLevelForStats(Stats stats) {
    if ((stats.mean() >= threshold) == alertWhenMoreThan) {
      return RiskLevel.HIGH;
    }
    return RiskLevel.LOW;
  }

  @Override
  public String getDisplayValueForStats(Stats stats) {
    return String.valueOf(DoubleMath.roundToInt(stats.mean(), RoundingMode.HALF_UP));
  }

  @Override
  public ArrayList<BucketData> generateDisplayData(int bucketSize, TimeUnit bucketTimeUnit) {
    ArrayList<BucketData> outputList = new ArrayList<>();
    long bucketSizeInMillis = bucketTimeUnit.toMillis(bucketSize);
    TreeMap<Long, Stats> statsTreeMap = super.generateBuckets(bucketSize, bucketTimeUnit);
    for (Long key : statsTreeMap.keySet()) {
      Stats stats = statsTreeMap.get(key);
      RiskLevel riskLevel = generateRiskLevelForStats(stats);
      String displayValue = getDisplayValueForStats(stats);
      BucketData bucketData = new BucketData(key, key + bucketSizeInMillis, riskLevel, stats, displayValue);
      outputList.add(bucketData);
    }
    return outputList;
  }

  public double getThreshold() {
    return threshold;
  }

  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  public boolean isAlertWhenMoreThan() {
    return alertWhenMoreThan;
  }

  public void setAlertWhenMoreThan(boolean alertWhenMoreThan) {
    this.alertWhenMoreThan = alertWhenMoreThan;
  }
}
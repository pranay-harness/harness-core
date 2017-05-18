package software.wings.service.impl.appdynamics;

/**
 * Created by rsingh on 5/17/17.
 */
public class AppdynamicsMetricDataValue {
  private long startTimeInMillis;
  private long value;
  private long min;
  private long max;
  private long current;
  private long sum;
  private long count;
  private double standardDeviation;
  private int occurrences;
  private boolean useRange;

  public long getStartTimeInMillis() {
    return startTimeInMillis;
  }

  public void setStartTimeInMillis(long startTimeInMillis) {
    this.startTimeInMillis = startTimeInMillis;
  }

  public long getValue() {
    return value;
  }

  public void setValue(long value) {
    this.value = value;
  }

  public long getMin() {
    return min;
  }

  public void setMin(long min) {
    this.min = min;
  }

  public long getMax() {
    return max;
  }

  public void setMax(long max) {
    this.max = max;
  }

  public long getCurrent() {
    return current;
  }

  public void setCurrent(long current) {
    this.current = current;
  }

  public long getSum() {
    return sum;
  }

  public void setSum(long sum) {
    this.sum = sum;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public double getStandardDeviation() {
    return standardDeviation;
  }

  public void setStandardDeviation(double standardDeviation) {
    this.standardDeviation = standardDeviation;
  }

  public int getOccurrences() {
    return occurrences;
  }

  public void setOccurrences(int occurrences) {
    this.occurrences = occurrences;
  }

  public boolean isUseRange() {
    return useRange;
  }

  public void setUseRange(boolean useRange) {
    this.useRange = useRange;
  }

  @Override
  public String toString() {
    return "AppdynamicsMetricDataValue{"
        + "startTimeInMillis=" + startTimeInMillis + ", value=" + value + ", min=" + min + ", max=" + max
        + ", current=" + current + ", sum=" + sum + ", count=" + count + ", standardDeviation=" + standardDeviation
        + ", occurrences=" + occurrences + ", useRange=" + useRange + '}';
  }
}

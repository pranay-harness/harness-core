package software.wings.metrics;

import com.github.reinert.jjschema.Attributes;
import software.wings.common.UUIDGenerator;

import com.google.common.math.Stats;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike@ on 4/7/17.
 */
public abstract class Metric<T extends Number> {
  @Attributes(required = true, title = "Uuid", description = "UUID") protected String uuid;
  @Attributes(required = true, title = "Name", description = "Average Response Time") protected String name;
  @Attributes(
      required = true, title = "Path", description = "Overall Application Performance|Average Response Time (ms)")
  protected String path;
  @Attributes(required = true, title = "Type", description = "Type of metric (e.g. Time, Count)")
  protected MetricType type;
  @Attributes(required = true, title = "Active", description = "Is metric actively analyzed") protected boolean active;
  @Attributes(required = true, title = "Values", description = "Map of timestamp to value")
  protected TreeMap<Long, T> values;

  public Metric(String name, String path, MetricType type, boolean active) {
    this.uuid = UUIDGenerator.getUuid();
    this.name = name;
    this.path = path;
    this.type = type;
    this.active = active;
    this.values = new TreeMap<>();
  }

  /**
   * Removes the oldest entries from the list until all elements in the list are within number timeUnits of the most
   * recent element.
   * @param number the number of timeUnits to include in the range
   * @param timeUnit the type of time unit (e.g. MINUTES, MILLISECONDS)
   */
  public void truncateByAge(long number, TimeUnit timeUnit) {
    int size = this.values.size();
    if (size == 0) {
      return;
    }
    long begin = this.values.lastKey() - timeUnit.toMillis(number);
    values.headMap(begin - 1).clear();
  }

  public TreeMap<Long, Stats> generateBuckets(int bucketSize, TimeUnit bucketTimeUnit) {
    TreeMap<Long, Stats> output = new TreeMap<>();
    long bucketSizeInMillis = bucketTimeUnit.toMillis(bucketSize);
    long endTime = this.values.firstKey();
    Long loopTime = this.values.lastKey();
    while (loopTime >= endTime) {
      loopTime -= bucketSizeInMillis;
      Stats stats = Stats.of(this.values.subMap(loopTime, false, loopTime + bucketSizeInMillis, true).values());
      output.put(loopTime, stats);
    }
    return output;
  }

  public abstract ArrayList<BucketData> generateDisplayData(int bucketSize, TimeUnit bucketTimeUnit);

  public abstract RiskLevel generateRiskLevelForStats(Stats stats);

  public abstract String getDisplayValueForStats(Stats stats);

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public void add(T value, long timestampInMillis) {
    values.put(timestampInMillis, value);
  }

  public void add(T value) {
    values.put(System.currentTimeMillis(), value);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public MetricType getType() {
    return type;
  }

  public void setType(MetricType type) {
    this.type = type;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public TreeMap<Long, T> getValues() {
    return values;
  }

  public void setValues(TreeMap<Long, T> values) {
    this.values = values;
  }
}

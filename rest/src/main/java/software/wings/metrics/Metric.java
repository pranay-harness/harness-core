package software.wings.metrics;

import com.github.reinert.jjschema.Attributes;
import com.google.common.math.Stats;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike@ on 4/7/17.
 */
public class Metric<T extends Number> {
  @Attributes(required = true, title = "Name", description = "Average Response Time") protected String name;
  @Attributes(
      required = true, title = "Path", description = "Overall Application Performance|Average Response Time (ms)")
  protected String path;
  @Attributes(required = true, title = "Type", description = "Type of metric (e.g. Time, Count)")
  protected MetricType type;
  @Attributes(required = true, title = "Values", description = "Map of timestamp to value")
  protected TreeMap<Long, T> values;

  public Metric(String name, String path, MetricType type) {
    this.name = name;
    this.path = path;
    this.type = type;
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

  public TreeMap<Long, T> getValues() {
    return values;
  }

  public void setValues(TreeMap<Long, T> values) {
    this.values = values;
  }
}

package software.wings.service.impl.appdynamics;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.MetricType;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Created by mike@ on 5/19/17.
 */
@Entity(value = "appdynamicsMetrics", noClassnameStored = true)
@Indexes({
  @Index(fields =
      {
        @Field("accountId")
        , @Field("appdynamicsAppId"), @Field("metricId"), @Field("tierId"), @Field("btName"), @Field("nodeName"),
            @Field("startTimeInMillis")
      },
      options = @IndexOptions(unique = true))
  ,
      @Index(fields = {
        @Field("accountId")
        , @Field("appdynamicsAppId"), @Field("metricId"), @Field("tierId"), @Field("btName"), @Field("nodeName")
      }), @Index(fields = {
        @Field("accountId"), @Field("appdynamicsAppId"), @Field("metricId"), @Field("tierId"), @Field("btName")
      }), @Index(fields = {
        @Field("accountId"), @Field("appdynamicsAppId"), @Field("metricId"), @Field("tierId")
      }), @Index(fields = { @Field("accountId")
                            , @Field("appdynamicsAppId"), @Field("metricId") })
})
public class AppdynamicsMetricDataRecord extends Base {
  // TODO: needs mapping from appd identifiers to harness id

  @NotEmpty private String accountId;
  @NotNull private long appdynamicsAppId;
  @NotEmpty private String metricName;
  @NotNull private long metricId;
  @NotNull private MetricType metricType;
  @NotNull private long tierId;
  @NotEmpty private String tierName;
  @NotEmpty private long btId;
  @NotEmpty private String btName;
  @NotEmpty private String nodeName;
  @NotEmpty private long startTimeInMillis;
  private long value;
  private long min;
  private long max;
  private long current;
  private long sum;
  private long count;
  private double standardDeviation;
  private int occurrences;
  private boolean useRange;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public long getAppdynamicsAppId() {
    return appdynamicsAppId;
  }

  public void setAppdynamicsAppId(long appdynamicsAppId) {
    this.appdynamicsAppId = appdynamicsAppId;
  }

  public String getMetricName() {
    return metricName;
  }

  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  public long getMetricId() {
    return metricId;
  }

  public void setMetricId(long metricId) {
    this.metricId = metricId;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }

  public long getTierId() {
    return tierId;
  }

  public void setTierId(long tierId) {
    this.tierId = tierId;
  }

  public String getTierName() {
    return tierName;
  }

  public void setTierName(String tierName) {
    this.tierName = tierName;
  }

  public long getBtId() {
    return btId;
  }

  public void setBtId(long btId) {
    this.btId = btId;
  }

  public String getBtName() {
    return btName;
  }

  public void setBtName(String btName) {
    this.btName = btName;
  }

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

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
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AppdynamicsMetricDataRecord that = (AppdynamicsMetricDataRecord) o;
    if (appdynamicsAppId != that.appdynamicsAppId)
      return false;
    if (metricId != that.metricId)
      return false;
    if (tierId != that.tierId)
      return false;
    if (startTimeInMillis != that.startTimeInMillis)
      return false;
    if (value != that.value)
      return false;
    if (min != that.min)
      return false;
    if (max != that.max)
      return false;
    if (current != that.current)
      return false;
    if (sum != that.sum)
      return false;
    if (count != that.count)
      return false;
    if (Double.compare(that.standardDeviation, standardDeviation) != 0)
      return false;
    if (occurrences != that.occurrences)
      return false;
    if (useRange != that.useRange)
      return false;
    if (!accountId.equals(that.accountId))
      return false;
    if (!metricName.equals(that.metricName))
      return false;
    if (metricType != that.metricType)
      return false;
    if (!tierName.equals(that.tierName))
      return false;
    if (!btName.equals(that.btName))
      return false;
    if (btId != that.btId)
      return false;
    return nodeName.equals(that.nodeName);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    long temp;
    result = 31 * result + accountId.hashCode();
    result = 31 * result + (int) (appdynamicsAppId ^ (appdynamicsAppId >>> 32));
    result = 31 * result + metricName.hashCode();
    result = 31 * result + (int) (metricId ^ (metricId >>> 32));
    result = 31 * result + metricType.hashCode();
    result = 31 * result + (int) (tierId ^ (tierId >>> 32));
    result = 31 * result + (int) (btId ^ (btId >>> 32));
    result = 31 * result + tierName.hashCode();
    result = 31 * result + btName.hashCode();
    result = 31 * result + nodeName.hashCode();
    result = 31 * result + (int) (startTimeInMillis ^ (startTimeInMillis >>> 32));
    result = 31 * result + (int) (value ^ (value >>> 32));
    result = 31 * result + (int) (min ^ (min >>> 32));
    result = 31 * result + (int) (max ^ (max >>> 32));
    result = 31 * result + (int) (current ^ (current >>> 32));
    result = 31 * result + (int) (sum ^ (sum >>> 32));
    result = 31 * result + (int) (count ^ (count >>> 32));
    temp = Double.doubleToLongBits(standardDeviation);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + occurrences;
    result = 31 * result + (useRange ? 1 : 0);
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accountId", accountId)
        .add("appdynamicsAppId", appdynamicsAppId)
        .add("metricName", metricName)
        .add("metricId", metricId)
        .add("metricType", metricType)
        .add("btId", btId)
        .add("tierId", tierId)
        .add("tierName", tierName)
        .add("btName", btName)
        .add("nodeName", nodeName)
        .add("startTimeInMillis", startTimeInMillis)
        .add("value", value)
        .add("min", min)
        .add("max", max)
        .add("current", current)
        .add("sum", sum)
        .add("count", count)
        .add("sdev", standardDeviation)
        .add("occurrences", occurrences)
        .add("useRange", useRange)
        .toString();
  }

  public static List<AppdynamicsMetricDataRecord> generateDataRecords(
      String accountId, long appdynamicsAppId, long tierId, AppdynamicsMetricData metricData) {
    /*
     * An AppDynamics metric path looks like:
     * "Business Transaction Performance|Business Transactions|test-tier|/todolist/|Individual Nodes|test-node|Number of
     * Slow Calls" Element 0 and 1 are constant Element 2 is the tier name Element 3 is the BT name Element 4 is
     * constant Element 5 is the node name Element 6 is the metric name
     */

    String[] appdynamicsPathPieces = metricData.getMetricPath().split(Pattern.quote("|"));
    String metricName = appdynamicsPathPieces[6];
    String tierName = appdynamicsPathPieces[2];
    String btName = appdynamicsPathPieces[3];
    String nodeName = appdynamicsPathPieces[5];
    /*
     * An AppDynamics metric name looks like:
     * "BTM|BTs|BT:132632|Component:42159|Average Response Time (ms)"
     * so right now we only care about element 2, the BT Id, and we delete the first three characters.
     */
    String[] metricNamePathPieces = metricData.getMetricName().split(Pattern.quote("|"));
    long btId = Long.parseLong(metricNamePathPieces[2].substring(3));
    List<AppdynamicsMetricDataRecord> records =
        Arrays.stream(metricData.getMetricValues())
            .map(value
                -> Builder.anAppdynamicsMetricsDataRecord()
                       .withAccountId(accountId)
                       .withAppdynamicsAppId(appdynamicsAppId)
                       .withMetricName(metricName)
                       .withMetricId(metricData.getMetricId())
                       .withMetricType(MetricType.COUNT) // TODO: needs real mapper
                       .withTierId(tierId)
                       .withTierName(tierName)
                       .withBtId(btId)
                       .withBtName(btName)
                       .withNodeName(nodeName)
                       .withStartTimeInMillis(value.getStartTimeInMillis())
                       .withValue(value.getValue())
                       .withMin(value.getMin())
                       .withMax(value.getMax())
                       .withCurrent(value.getCurrent())
                       .withSum(value.getSum())
                       .withCount(value.getCount())
                       .withStandardDeviation(value.getStandardDeviation())
                       .withOccurrences(value.getOccurrences())
                       .withUseRange(value.isUseRange())
                       .build())
            .collect(Collectors.toList());
    return records;
  }

  public static final class Builder {
    private String accountId;
    private long appdynamicsAppId;
    private String metricName;
    private long metricId;
    private MetricType metricType;
    private long tierId;
    private String tierName;
    private long btId;
    private String btName;
    private String nodeName;
    private long startTimeInMillis;
    private long value;
    private long min;
    private long max;
    private long current;
    private long sum;
    private long count;
    private double standardDeviation;
    private int occurrences;
    private boolean useRange = true;

    private Builder() {}

    /**
     * an AppdynamicsMetricsDataRecord base builder.
     *
     * @return the builder
     */
    public static Builder anAppdynamicsMetricsDataRecord() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withAppdynamicsAppId(long appdynamicsAppId) {
      this.appdynamicsAppId = appdynamicsAppId;
      return this;
    }

    public Builder withMetricName(String metricName) {
      this.metricName = metricName;
      return this;
    }

    public Builder withMetricId(long metricId) {
      this.metricId = metricId;
      return this;
    }

    public Builder withMetricType(MetricType metricType) {
      this.metricType = metricType;
      return this;
    }

    public Builder withTierId(long tierId) {
      this.tierId = tierId;
      return this;
    }

    public Builder withTierName(String tierName) {
      this.tierName = tierName;
      return this;
    }

    public Builder withBtId(long btId) {
      this.btId = btId;
      return this;
    }

    public Builder withBtName(String btName) {
      this.btName = btName;
      return this;
    }

    public Builder withNodeName(String nodeName) {
      this.nodeName = nodeName;
      return this;
    }

    public Builder withStartTimeInMillis(long startTimeInMillis) {
      this.startTimeInMillis = startTimeInMillis;
      return this;
    }

    public Builder withValue(long value) {
      this.value = value;
      return this;
    }

    public Builder withMin(long min) {
      this.min = min;
      return this;
    }

    public Builder withMax(long max) {
      this.max = max;
      return this;
    }

    public Builder withCurrent(long current) {
      this.current = current;
      return this;
    }

    public Builder withSum(long sum) {
      this.sum = sum;
      return this;
    }

    public Builder withCount(long count) {
      this.count = count;
      return this;
    }

    public Builder withStandardDeviation(double standardDeviation) {
      this.standardDeviation = standardDeviation;
      return this;
    }

    public Builder withOccurrences(int occurrences) {
      this.occurrences = occurrences;
      return this;
    }

    public Builder withUseRange(boolean useRange) {
      this.useRange = useRange;
      return this;
    }

    public Builder but() {
      return anAppdynamicsMetricsDataRecord()
          .withAccountId(accountId)
          .withAppdynamicsAppId(appdynamicsAppId)
          .withMetricName(metricName)
          .withMetricId(metricId)
          .withMetricType(metricType)
          .withTierId(tierId)
          .withTierName(tierName)
          .withBtId(btId)
          .withBtName(btName)
          .withNodeName(nodeName)
          .withStartTimeInMillis(startTimeInMillis)
          .withValue(value)
          .withMin(min)
          .withMax(max)
          .withCurrent(current)
          .withSum(sum)
          .withCount(count)
          .withStandardDeviation(standardDeviation)
          .withOccurrences(occurrences)
          .withUseRange(useRange);
    }

    public AppdynamicsMetricDataRecord build() {
      AppdynamicsMetricDataRecord appdynamicsMetricDataRecord = new AppdynamicsMetricDataRecord();
      appdynamicsMetricDataRecord.setAccountId(accountId);
      appdynamicsMetricDataRecord.setAppdynamicsAppId(appdynamicsAppId);
      appdynamicsMetricDataRecord.setMetricName(metricName);
      appdynamicsMetricDataRecord.setMetricId(metricId);
      appdynamicsMetricDataRecord.setMetricType(metricType);
      appdynamicsMetricDataRecord.setTierId(tierId);
      appdynamicsMetricDataRecord.setTierName(tierName);
      appdynamicsMetricDataRecord.setBtId(btId);
      appdynamicsMetricDataRecord.setBtName(btName);
      appdynamicsMetricDataRecord.setNodeName(nodeName);
      appdynamicsMetricDataRecord.setStartTimeInMillis(startTimeInMillis);
      appdynamicsMetricDataRecord.setValue(value);
      appdynamicsMetricDataRecord.setMin(min);
      appdynamicsMetricDataRecord.setMax(max);
      appdynamicsMetricDataRecord.setCurrent(current);
      appdynamicsMetricDataRecord.setSum(sum);
      appdynamicsMetricDataRecord.setCount(count);
      appdynamicsMetricDataRecord.setStandardDeviation(standardDeviation);
      appdynamicsMetricDataRecord.setOccurrences(occurrences);
      appdynamicsMetricDataRecord.setUseRange(useRange);
      return appdynamicsMetricDataRecord;
    }
  }
}

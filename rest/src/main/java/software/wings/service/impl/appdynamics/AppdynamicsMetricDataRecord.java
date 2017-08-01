package software.wings.service.impl.appdynamics;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.metrics.MetricType;
import software.wings.security.annotations.Archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Created by mike@ on 5/19/17.
 */
@Data
@Entity(value = "appdynamicsMetrics", noClassnameStored = true)
@Indexes({
  @Index(fields =
      {
        @Field("accountId")
        , @Field("appdAppId"), @Field("metricId"), @Field("tierId"), @Field("btName"), @Field("nodeName"),
            @Field("startTime")
      },
      options = @IndexOptions(unique = true, name = "appDynamicsMetricsUniqueIdx"))
  ,
      @Index(fields = {
        @Field("accountId")
        , @Field("appdAppId"), @Field("metricId"), @Field("tierId"), @Field("btName"), @Field("nodeName")
      }), @Index(fields = {
        @Field("accountId"), @Field("appdAppId"), @Field("metricId"), @Field("tierId"), @Field("btName")
      }), @Index(fields = {
        @Field("accountId"), @Field("appdAppId"), @Field("metricId"), @Field("tierId")
      }), @Index(fields = { @Field("accountId")
                            , @Field("appdAppId"), @Field("metricId") })
})
@Archive(retentionMills = 7 * 24 * 60 * 60 * 1000)
public class AppdynamicsMetricDataRecord extends Base {
  // TODO: needs mapping from appd identifiers to harness id

  @NotEmpty private String accountId;
  @NotEmpty private String applicationId;
  @NotNull private long appdAppId;
  @NotEmpty private String metricName;
  @NotNull private long metricId;
  @NotNull private MetricType metricType;
  @NotNull private long tierId;
  @NotEmpty private String tierName;
  @NotEmpty private long btId;
  @NotEmpty private String btName;
  @NotEmpty private String nodeName;
  @NotEmpty private long startTime;
  @NotEmpty private String stateExecutionId;
  private double value;
  private double min;
  private double max;
  private double current;
  private double sum;
  private long count;
  private double standardDeviation;
  private int occurrences;
  private boolean useRange;

  public static List<AppdynamicsMetricDataRecord> generateDataRecords(String accountId, String applicationId,
      String stateExecutionId, long appdynamicsAppId, long tierId, AppdynamicsMetricData metricData) {
    if (metricData.getMetricName().equals("METRIC DATA NOT FOUND")) {
      return new ArrayList<>();
    }
    /*
     * An AppDynamics metric path looks like:
     * "Business Transaction Performance|Business Transactions|test-tier|/todolist/|Individual Nodes|test-node|Number of
     * Slow Calls" Element 0 and 1 are constant Element 2 is the tier name Element 3 is the BT name Element 4 is
     * constant Element 5 is the node name Element 6 is the metric name
     */

    String[] appdynamicsPathPieces = metricData.getMetricPath().split(Pattern.quote("|"));
    String metricName = parseAppdynamicsInternalName(appdynamicsPathPieces, 6);
    String tierName = parseAppdynamicsInternalName(appdynamicsPathPieces, 2);
    String btName = parseAppdynamicsInternalName(appdynamicsPathPieces, 3);
    String nodeName = parseAppdynamicsInternalName(appdynamicsPathPieces, 5);

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
                       .withApplicationId(applicationId)
                       .withStateExecutionId(stateExecutionId)
                       .withAppdAppId(appdynamicsAppId)
                       .withMetricName(metricName)
                       .withMetricId(metricData.getMetricId())
                       .withMetricType(MetricType.COUNT) // TODO: needs real mapper
                       .withTierId(tierId)
                       .withTierName(tierName)
                       .withBtId(btId)
                       .withBtName(btName)
                       .withNodeName(nodeName)
                       .withStartTime(value.getStartTimeInMillis())
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

  private static String parseAppdynamicsInternalName(String[] appdynamicsPathPieces, int index) {
    String name = appdynamicsPathPieces[index];
    if (name == null) {
      return name;
    }

    // mongo doesn't like dots in the names
    return name.replaceAll("\\.", "-");
  }

  public static final class Builder {
    private String accountId;
    private String applicationId;
    private String stateExecutionId;
    private long appdAppId;
    private String metricName;
    private long metricId;
    private MetricType metricType;
    private long tierId;
    private String tierName;
    private long btId;
    private String btName;
    private String nodeName;
    private long startTime;
    private double value;
    private double min;
    private double max;
    private double current;
    private double sum;
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

    public Builder withApplicationId(String applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withStateExecutionId(String stateExecutionId) {
      this.stateExecutionId = stateExecutionId;
      return this;
    }

    public Builder withAppdAppId(long appdynamicsAppId) {
      this.appdAppId = appdynamicsAppId;
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

    public Builder withStartTime(long startTimeInMillis) {
      this.startTime = startTimeInMillis;
      return this;
    }

    public Builder withValue(double value) {
      this.value = value;
      return this;
    }

    public Builder withMin(double min) {
      this.min = min;
      return this;
    }

    public Builder withMax(double max) {
      this.max = max;
      return this;
    }

    public Builder withCurrent(double current) {
      this.current = current;
      return this;
    }

    public Builder withSum(double sum) {
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
          .withAppdAppId(appdAppId)
          .withMetricName(metricName)
          .withMetricId(metricId)
          .withMetricType(metricType)
          .withTierId(tierId)
          .withTierName(tierName)
          .withBtId(btId)
          .withBtName(btName)
          .withNodeName(nodeName)
          .withStartTime(startTime)
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
      appdynamicsMetricDataRecord.setApplicationId(applicationId);
      appdynamicsMetricDataRecord.setStateExecutionId(stateExecutionId);
      appdynamicsMetricDataRecord.setAppdAppId(appdAppId);
      appdynamicsMetricDataRecord.setMetricName(metricName);
      appdynamicsMetricDataRecord.setMetricId(metricId);
      appdynamicsMetricDataRecord.setMetricType(metricType);
      appdynamicsMetricDataRecord.setTierId(tierId);
      appdynamicsMetricDataRecord.setTierName(tierName);
      appdynamicsMetricDataRecord.setBtId(btId);
      appdynamicsMetricDataRecord.setBtName(btName);
      appdynamicsMetricDataRecord.setNodeName(nodeName);
      appdynamicsMetricDataRecord.setStartTime(startTime);
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

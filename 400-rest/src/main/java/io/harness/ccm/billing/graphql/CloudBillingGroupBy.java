package io.harness.ccm.billing.graphql;

import io.harness.ccm.billing.RawBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;

import lombok.Data;

@Data
public class CloudBillingGroupBy {
  private CloudEntityGroupBy entityGroupBy;
  private TimeTruncGroupby timeTruncGroupby;

  private static final String startTimeTruncAlias = "start_time_trunc";

  // convert groupBy from QL context to SQL context
  public Object toGroupbyObject() {
    if (entityGroupBy != null) {
      return entityGroupBy.getDbObject();
    }
    if (timeTruncGroupby != null) {
      timeTruncGroupby.setEntity(PreAggregatedTableSchema.startTime);
      timeTruncGroupby.setAlias(startTimeTruncAlias); // the default value would be different for different context
      return timeTruncGroupby.toGroupbyObject();
    }
    return null;
  }

  public Object toRawTableGroupbyObject() {
    if (entityGroupBy != null) {
      return entityGroupBy.getRawDbObject();
    }
    if (timeTruncGroupby != null) {
      timeTruncGroupby.setAlias(startTimeTruncAlias);
      timeTruncGroupby.setEntity(RawBillingTableSchema.startTime);
      return timeTruncGroupby.toGroupbyObject();
    }
    return null;
  }

  public Object toAwsRawTableGroupbyObject() {
    if (entityGroupBy != null) {
      return entityGroupBy.getAwsRawDbObject();
    }
    if (timeTruncGroupby != null) {
      timeTruncGroupby.setAlias(startTimeTruncAlias);
      timeTruncGroupby.setEntity(RawBillingTableSchema.awsStartTime);
      return timeTruncGroupby.toGroupbyObject();
    }
    return null;
  }
}

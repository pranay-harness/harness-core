package io.harness.ccm.billing.graphql;

import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;
import lombok.Data;

@Data
public class CloudBillingGroupBy {
  private CloudEntityGroupBy entityGroupBy;
  private TimeTruncGroupby timeTruncGroupby;

  // convert groupBy from QL context to SQL context
  public Object toGroupbyObject() {
    if (entityGroupBy != null) {
      return entityGroupBy.getDbObject();
    }
    if (timeTruncGroupby != null) {
      timeTruncGroupby.setEntity(PreAggregatedTableSchema.startTime);
      timeTruncGroupby.setAlias("start_time_trunc"); // the default value would be different for different context
      return timeTruncGroupby.toGroupbyObject();
    }
    return null;
  }
}

package io.harness.ccm.billing.graphql;

import io.harness.ccm.billing.GcpBillingTableSchema;
import lombok.Data;

@Data
public class GcpBillingGroupby {
  private GcpBillingEntityGroupby entityGroupBy;
  private TimeTruncGroupby timeTruncGroupby;

  // convert groupBy from QL context to SQL context
  public Object toGroupbyObject() {
    if (entityGroupBy != null) {
      return entityGroupBy.getDbObject();
    }
    if (timeTruncGroupby != null) {
      timeTruncGroupby.setEntity(GcpBillingTableSchema.usageStartTime);
      timeTruncGroupby.setAlias("start_time_trunc"); // the default value would be different for different context
      return timeTruncGroupby.toGroupbyObject();
    }
    return null;
  }
}

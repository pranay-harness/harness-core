/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.RawBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;

import lombok.Data;

@Data
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
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
  public boolean isEntityGroupBY() {
    return entityGroupBy != null;
  }
  public boolean isTimeGroupBY() {
    return timeTruncGroupby != null;
  }
}

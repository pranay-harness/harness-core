package io.harness.ccm.billing;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.Builder;

@Builder
public class GcpBillingEntityStatsDTO implements QLData {
  List<GcpBillingEntityDataPoints> data;
}

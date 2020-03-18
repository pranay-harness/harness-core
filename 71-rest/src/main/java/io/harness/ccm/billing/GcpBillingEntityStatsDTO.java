package io.harness.ccm.billing;

import lombok.Builder;
import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;

@Builder
public class GcpBillingEntityStatsDTO implements QLData {
  List<GcpBillingEntityDataPoints> data;
}

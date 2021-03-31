package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.Builder;

@Builder
@OwnedBy(CE)
public class GcpBillingEntityStatsDTO implements QLData {
  List<GcpBillingEntityDataPoints> data;
}

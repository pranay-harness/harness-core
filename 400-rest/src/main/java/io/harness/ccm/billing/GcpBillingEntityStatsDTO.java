package io.harness.ccm.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.Builder;

@Builder
@TargetModule(Module._490_CE_COMMONS)
public class GcpBillingEntityStatsDTO implements QLData {
  List<GcpBillingEntityDataPoints> data;
}

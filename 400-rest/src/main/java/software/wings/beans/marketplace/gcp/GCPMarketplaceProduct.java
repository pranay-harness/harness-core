/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class GCPMarketplaceProduct {
  String product;
  String plan;
  String quoteId;
  String usageReportingId;
  Instant startTime;
  Instant lastUsageReportTime;
}

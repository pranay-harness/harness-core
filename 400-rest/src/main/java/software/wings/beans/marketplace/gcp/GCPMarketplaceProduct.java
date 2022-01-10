/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

package software.wings.beans.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

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

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.marketplace.gcp.GCPUsageReport;

import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This service is used for GCP usage.
 */
@OwnedBy(PL)
@ParametersAreNonnullByDefault
public interface GCPUsageReportService {
  /**
   * Save an instance of {@link GCPUsageReport}
   */
  boolean create(GCPUsageReport gcpUsageReport);

  /**
   * Get the last time gcpUsageReport were saved for this account
   */
  @Nullable Instant fetchLastGCPUsageReportTime(String accountId);

  /**
   * Get instance snapshot for time range for this account
   * @param from - exclusive
   * @param to - inclusive
   */
  @Nullable List<GCPUsageReport> fetchGCPUsageReport(String accountId, Instant from, Instant to);
}

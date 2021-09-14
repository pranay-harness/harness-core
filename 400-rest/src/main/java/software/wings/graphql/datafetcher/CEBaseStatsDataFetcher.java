/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface CEBaseStatsDataFetcher {
  /**
   * returning true will change the value of accountId to sampleAccountId (from config.yml) so that the data is
   * fetched for sampleAccountId.
   *
   * return false when in doubt.
   * @author UTSAV
   */
  boolean isCESampleAccountIdAllowed();
}

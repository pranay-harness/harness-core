/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.contracts.PollingPayloadData;

@OwnedBy(HarnessTeam.CDC)
public interface PollingInfoBuilder {
  PollingInfo toPollingInfo(PollingPayloadData pollingPayloadData);
}

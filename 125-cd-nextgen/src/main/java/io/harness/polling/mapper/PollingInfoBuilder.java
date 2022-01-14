package io.harness.polling.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.contracts.PollingPayloadData;

@OwnedBy(HarnessTeam.CDC)
public interface PollingInfoBuilder {
  PollingInfo toPollingInfo(PollingPayloadData pollingPayloadData);
}

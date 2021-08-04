package io.harness.ngtriggers.buildtriggers.helpers.generator;

import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.polling.contracts.PollingItem;

public interface PollingItemGenerator {
  PollingItem generatePollingItem(BuildTriggerOpsData buildTriggerOpsData);
}

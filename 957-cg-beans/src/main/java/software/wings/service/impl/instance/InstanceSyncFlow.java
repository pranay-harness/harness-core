package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP) public enum InstanceSyncFlow { NEW_DEPLOYMENT, PERPETUAL_TASK, ITERATOR, MANUAL }

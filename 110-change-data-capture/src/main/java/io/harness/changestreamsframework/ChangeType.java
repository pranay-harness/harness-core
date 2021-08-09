package io.harness.changestreamsframework;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
@OwnedBy(HarnessTeam.CE) public enum ChangeType { INSERT, DELETE, REPLACE, UPDATE, INVALIDATE }

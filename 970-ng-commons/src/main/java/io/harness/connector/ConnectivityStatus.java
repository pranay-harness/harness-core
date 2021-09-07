package io.harness.connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX) public enum ConnectivityStatus { SUCCESS, FAILURE, PARTIAL, UNKNOWN }
